package com.dodo.backend.auth.service;

import com.dodo.backend.auth.client.SocialApiClient;
import com.dodo.backend.auth.dto.request.AuthRequest.SocialLoginRequest;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialLoginResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialRegisterResponse;
import com.dodo.backend.auth.entity.RefreshToken;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.auth.repository.RefreshTokenRepository;
import com.dodo.backend.common.jwt.JwtTokenProvider;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.auth.exception.AuthErrorCode.INVALID_REQUEST;
import static com.dodo.backend.auth.exception.AuthErrorCode.TOO_MANY_REQUESTS;

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
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 요청된 Provider(NAVER, GOOGLE)를 지원하는 ApiClient 전략 선택 (지원 불가 시 예외 발생)
     * 2. 외부 API 통신을 통해 AccessToken 발급 및 유저 프로필 조회
     * 3. 이메일 기반으로 유저 상태 조회 (신규/기존) 및 DB 처리 위임
     * 4. 유저 상태에 따라 회원가입용 임시 토큰 또는 로그인용 정식 토큰 발급
     * <p>
     * (참고: provider, code의 null 여부는 컨트롤러단에서 @Valid를 통해 사전에 검증됩니다.)
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
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. {@link RateLimitService}를 통해 해당 IP의 차단(Ban) 여부를 우선 확인
     * 2. 이미 차단된 IP일 경우 즉시 {@code TOO_MANY_REQUESTS} 예외를 발생시켜 접근 제어
     * 3. 차단되지 않은 경우 시도 횟수를 증가시키며, 횟수가 임계치(5회)에 도달하면 10분간 IP 차단 및 기록 삭제 수행
     *
     * @param clientIp 요청자의 IP 주소
     * @throws AuthException IP가 차단 상태이거나 단시간 내 요청 횟수를 초과한 경우({@code TOO_MANY_REQUESTS}) 발생
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
}