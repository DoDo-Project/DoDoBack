package com.dodo.backend.auth.service;

import com.dodo.backend.auth.client.SocialApiClient;
import com.dodo.backend.auth.dto.request.AuthRequest.LogoutRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.SocialLoginRequest;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialLoginResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialRegisterResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.TokenResponse;
import com.dodo.backend.auth.entity.RefreshToken;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.auth.repository.RefreshTokenRepository;
import com.dodo.backend.common.jwt.JwtTokenProvider;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.dodo.backend.auth.exception.AuthErrorCode.*;

/**
 * {@link AuthService}의 구현체로, 전략 패턴(Strategy Pattern)을 사용하여 소셜 로그인을 처리합니다.
 * <p>
 * {@link SocialApiClient} 인터페이스를 구현한 빈들 중
 * 요청된 Provider에 맞는 클라이언트를 동적으로 선택하여 인증을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final RateLimitService rateLimitService;
    private final List<SocialApiClient> socialApiClients;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 요청된 Provider(NAVER, GOOGLE)를 지원하는 ApiClient 전략 선택 (지원 불가 시 예외 발생)
     * 2. 외부 API 통신을 통해 AccessToken 발급 및 유저 프로필 조회
     * 3. 이메일 기반으로 유저 정보 조회 (UserService 호출)
     * 4. <b>조회된 유저의 상태 문자열(status)을 검증하여 제재된 계정일 경우 예외 발생 (ACCOUNT_RESTRICTED)</b>
     * 5. 유저 상태에 따라 회원가입용 임시 토큰 또는 로그인용 정식 토큰 발급
     */
    @Override
    public ResponseEntity<?> socialLogin(SocialLoginRequest request) {

        SocialApiClient client = socialApiClients.stream()
                .filter(c -> c.support(request.getProvider()))
                .findFirst()
                .orElseThrow(() -> new AuthException(INVALID_REQUEST));

        String socialAccessToken = client.getAccessToken(request.getCode());
        Map<String, Object> profile = client.getUserProfile(socialAccessToken);

        Map<String, Object> userInfo = userService.findOrSaveSocialUser(
                (String) profile.get("email"),
                (String) profile.get("name"),
                (String) profile.get("profileUrl")
        );

        String status = String.valueOf(userInfo.get("status"));
        validateUserStatus(status, (String) profile.get("email"));

        boolean isNewMember = (boolean) userInfo.get("isNewMember");

        if (isNewMember) {
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String registrationToken = jwtTokenProvider.createRegisterToken(email);

            return ResponseEntity.status(202).body(
                    SocialRegisterResponse.toDto(
                            "추가 정보가 필요합니다.",
                            email,
                            name,
                            registrationToken,
                            jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 2000
                    )
            );
        } else {
            UUID userId = (UUID) userInfo.get("userId");
            String role = (String) userInfo.get("role");
            String profileUrl = (String) userInfo.get("profileUrl");

            String accessToken = jwtTokenProvider.createAccessToken(userId, role);
            String refreshToken = jwtTokenProvider.createRefreshToken(userId);

            refreshTokenRepository.save(RefreshToken.builder()
                    .usersId(userId.toString())
                    .refreshToken(refreshToken)
                    .role(role)
                    .build());

            return ResponseEntity.ok(
                    SocialLoginResponse.toDto(
                            "로그인이 완료되었습니다.",
                            profileUrl,
                            accessToken,
                            refreshToken,
                            jwtTokenProvider.getAccessTokenValidityInMilliseconds()
                    )
            );
        }
    }

    /**
     * 유저의 계정 상태가 로그인 가능한 상태인지 검증합니다.
     * <p>
     * 문자열 비교를 통해 정지(SUSPENDED), 휴면(DORMANT), 삭제(DELETED) 상태일 경우
     * {@link AuthException}을 발생시킵니다.
     *
     * @param status 유저의 현재 상태 문자열
     * @param email  로그인을 시도하는 이메일 (로깅용)
     */
    private void validateUserStatus(String status, String email) {
        if ("SUSPENDED".equals(status)
                || "DORMANT".equals(status)
                || "DELETED".equals(status)) {
            log.warn("계정 사용 제한 유저 접속 시도 - 이메일: {}, 상태: {}", email, status);
            throw new AuthException(ACCOUNT_RESTRICTED);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. {@link RateLimitService}를 통해 해당 IP의 차단(Ban) 여부를 우선 확인
     * 2. 이미 차단된 IP일 경우 즉시 {@code TOO_MANY_REQUESTS} 예외를 발생시켜 접근 제어
     * 3. 차단되지 않은 경우 시도 횟수를 증가시키며, 횟수가 임계치(5회)에 도달하면 10분간 IP 차단 및 기록 삭제 수행
     */
    @Override
    public void checkRateLimit(String clientIp) {

        if (rateLimitService.isIpBanned(clientIp)) {
            log.warn("차단된 IP의 접근 시도 차단 - IP: {}", clientIp);
            throw new AuthException(TOO_MANY_REQUESTS);
        }

        Long count = rateLimitService.incrementAttempt(clientIp, 1);

        if (count != null && count >= 5) {
            log.warn("IP 차단 실행 (5회 초과) - IP: {}, 기간: 10분", clientIp);
            rateLimitService.banIp(clientIp, 10);
            rateLimitService.deleteAttempts(clientIp);
            throw new AuthException(TOO_MANY_REQUESTS);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 요청받은 Refresh Token을 이용해 Redis에서 저장된 토큰 정보를 조회합니다.
     * 2. 토큰이 존재하지 않을 경우 {@code TOKEN_NOT_FOUND} 예외를 발생시킵니다.
     * 3. 토큰이 존재하면 Redis에서 해당 데이터를 삭제하여 재발급을 불가능하게 만듭니다.
     * 4. Access Token의 남은 유효 시간을 계산하여 Redis 블랙리스트에 등록합니다.
     */
    @Transactional
    @Override
    public void logout(LogoutRequest request, String accessToken) {

        log.info("로그아웃 요청 수신");

        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException(TOKEN_NOT_FOUND));
        refreshTokenRepository.delete(refreshToken);

        long expiration = jwtTokenProvider.getRemainingValidTime(accessToken);
        if (expiration > 0) {
            redisTemplate.opsForValue()
                    .set("blacklist:" + accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
            log.info("Access Token 블랙리스트 등록 완료 (남은 시간: {}ms)", expiration);
        }

        log.info("로그아웃 완료 - User ID: {}", refreshToken.getUsersId());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 전달받은 Refresh Token의 유효성(서명, 만료 여부)을 JWT 자체 검증으로 확인
     * 2. Redis 조회: 해당 토큰이 실제로 저장되어 있는지 확인 (만료되거나 이미 사용된 경우 예외 발생)
     * 3. RTR 수행: 기존 토큰을 삭제하고, 동일한 유저 정보로 새로운 Access/Refresh Token 생성
     * 4. 새로운 Refresh Token을 Redis에 저장 및 응답 반환
     */
    @Transactional
    @Override
    public TokenResponse reissueToken(ReissueRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            log.warn("재발급 실패: 유효하지 않거나 만료된 Refresh Token입니다.");
            throw new AuthException(EXPIRED_REFRESH_TOKEN);
        }

        RefreshToken storedToken = refreshTokenRepository.findByRefreshToken(requestRefreshToken)
                .orElseThrow(() -> {
                    log.warn("재발급 실패: Redis에서 토큰을 찾을 수 없습니다.");
                    return new AuthException(TOKEN_NOT_FOUND);
                });

        refreshTokenRepository.delete(storedToken);

        UUID userId = UUID.fromString(storedToken.getUsersId());
        String role = storedToken.getRole();

        String newAccessToken = jwtTokenProvider.createAccessToken(userId, role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenRepository.save(RefreshToken.builder()
                .usersId(userId.toString())
                .refreshToken(newRefreshToken)
                .role(role)
                .build());

        log.info("토큰 재발급 성공 - User ID: {}", userId);

        long expiresInSeconds = jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000;

        return TokenResponse.toDto(newAccessToken, newRefreshToken, expiresInSeconds);
    }
}