package com.dodo.backend.auth.service;

import com.dodo.backend.auth.dto.request.AuthRequest.LogoutRequest;
import com.dodo.backend.auth.entity.RefreshToken;
import com.dodo.backend.auth.exception.AuthErrorCode;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.auth.repository.RefreshTokenRepository;
import com.dodo.backend.common.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link AuthServiceImpl}의 비즈니스 로직을 검증하는 단위 테스트 클래스입니다.
 * <p>
 * 주로 로그아웃 처리 시 리프레시 토큰 삭제 및 액세스 토큰의 블랙리스트 처리 로직을 중점적으로 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    /**
     * 로그아웃 성공 시나리오를 테스트합니다.
     * <p>
     * 1. DB에서 리프레시 토큰이 정상적으로 삭제되어야 합니다.<br>
     * 2. 남은 유효 기간이 있는 액세스 토큰은 Redis 블랙리스트에 등록되어야 합니다.
     */
    @Test
    @DisplayName("로그아웃 성공 - 리프레시 토큰 삭제 및 액세스 토큰 블랙리스트 등록")
    void logout_Success() {
        // given
        String refreshTokenStr = "valid-refresh-token";
        String accessTokenStr = "valid-access-token";

        String userId = UUID.randomUUID().toString();
        long remainingTime = 3600000L;

        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(refreshTokenStr)
                .build();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .usersId(userId)
                .refreshToken(refreshTokenStr)
                .build();

        given(refreshTokenRepository.findByRefreshToken(refreshTokenStr))
                .willReturn(Optional.of(refreshTokenEntity));

        given(jwtTokenProvider.getRemainingValidTime(accessTokenStr))
                .willReturn(remainingTime);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        authService.logout(request, accessTokenStr);

        // then
        verify(refreshTokenRepository).delete(refreshTokenEntity);

        verify(valueOperations).set(
                eq("blacklist:" + accessTokenStr),
                eq("logout"),
                eq(remainingTime),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    /**
     * 이미 만료된 액세스 토큰으로 로그아웃을 시도하는 시나리오를 테스트합니다.
     * <p>
     * 리프레시 토큰은 삭제되어야 하지만, 액세스 토큰은 이미 만료되었으므로
     * 불필요한 리소스 낭비를 막기 위해 블랙리스트에는 등록되지 않아야 합니다.
     */
    @Test
    @DisplayName("로그아웃 성공 - 액세스 토큰이 이미 만료된 경우 (블랙리스트 등록 안 함)")
    void logout_Success_ExpiredAccessToken() {
        // given
        String refreshTokenStr = "valid-refresh-token";
        String accessTokenStr = "expired-access-token";
        String userId = UUID.randomUUID().toString();

        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(refreshTokenStr)
                .build();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .usersId(userId)
                .refreshToken(refreshTokenStr)
                .build();

        given(refreshTokenRepository.findByRefreshToken(refreshTokenStr))
                .willReturn(Optional.of(refreshTokenEntity));

        given(jwtTokenProvider.getRemainingValidTime(accessTokenStr))
                .willReturn(0L);
        // when
        authService.logout(request, accessTokenStr);

        // then
        verify(refreshTokenRepository).delete(refreshTokenEntity);

        verify(redisTemplate, never()).opsForValue();
    }

    /**
     * 유효하지 않은(존재하지 않는) 리프레시 토큰으로 로그아웃을 시도하는 시나리오를 테스트합니다.
     * <p>
     * {@link AuthException} 예외가 발생해야 하며, 에러 코드는 {@link AuthErrorCode#TOKEN_NOT_FOUND}여야 합니다.
     */
    @Test
    @DisplayName("로그아웃 실패 - 존재하지 않는 리프레시 토큰")
    void logout_Fail_NotFoundRefreshToken() {
        // given
        String invalidRefreshToken = "invalid-token";
        String accessTokenStr = "any-access-token";

        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(invalidRefreshToken)
                .build();

        given(refreshTokenRepository.findByRefreshToken(invalidRefreshToken))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.logout(request, accessTokenStr))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.TOKEN_NOT_FOUND);

        verify(refreshTokenRepository, never()).delete(any());
        verify(redisTemplate, never()).opsForValue();
    }

    /**
     * 토큰 재발급 성공 시나리오를 테스트합니다.
     * <p>
     * 리프레시 토큰이 유효하고 Redis에 존재할 경우,
     * 기존 토큰을 삭제(RTR)하고 새로운 액세스/리프레시 토큰을 발급해야 합니다.
     */
    @Test
    @DisplayName("토큰 재발급 성공 - RTR 적용 및 신규 토큰 발급")
    void reissueToken_Success() {
        // given
        String oldRefreshTokenStr = "valid-old-refresh-token";
        String newAccessTokenStr = "new-access-token";
        String newRefreshTokenStr = "new-refresh-token";
        String userId = UUID.randomUUID().toString();
        String role = "USER";

        com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest request =
                new com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest(oldRefreshTokenStr);

        RefreshToken oldTokenEntity = RefreshToken.builder()
                .usersId(userId)
                .refreshToken(oldRefreshTokenStr)
                .role(role)
                .build();

        given(jwtTokenProvider.validateToken(oldRefreshTokenStr))
                .willReturn(true);
        given(refreshTokenRepository.findByRefreshToken(oldRefreshTokenStr))
                .willReturn(Optional.of(oldTokenEntity));
        given(jwtTokenProvider.createAccessToken(UUID.fromString(userId), role))
                .willReturn(newAccessTokenStr);
        given(jwtTokenProvider.createRefreshToken(UUID.fromString(userId)))
                .willReturn(newRefreshTokenStr);
        given(jwtTokenProvider.getAccessTokenValidityInMilliseconds())
                .willReturn(3600000L);

        // when
        com.dodo.backend.auth.dto.response.AuthResponse.TokenResponse response =
                authService.reissueToken(request);

        // then
        verify(refreshTokenRepository).delete(oldTokenEntity);
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        assertThat(response.getAccessToken()).isEqualTo(newAccessTokenStr);
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshTokenStr);
    }

    /**
     * 유효하지 않은 리프레시 토큰으로 재발급을 시도하는 시나리오를 테스트합니다.
     * <p>
     * 토큰 서명이 틀리거나 만료된 경우 {@link AuthException}(EXPIRED_REFRESH_TOKEN)이 발생해야 합니다.
     */
    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 Refresh Token")
    void reissueToken_Fail_InvalidToken() {
        // given
        String invalidToken = "invalid-token";
        com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest request =
                new com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest(invalidToken);

        given(jwtTokenProvider.validateToken(invalidToken))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissueToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.EXPIRED_REFRESH_TOKEN);

        verify(refreshTokenRepository, never()).delete(any());
    }

    /**
     * Redis에 저장되어 있지 않은 리프레시 토큰으로 재발급을 시도하는 시나리오를 테스트합니다.
     * <p>
     * 토큰 형식은 유효하지만 DB에 없는 경우(이미 로그아웃됨 등),
     * {@link AuthException}(TOKEN_NOT_FOUND)이 발생해야 합니다.
     */
    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 존재하지 않는 Refresh Token")
    void reissueToken_Fail_NotFoundInRedis() {
        // given
        String notFoundToken = "valid-format-token";
        com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest request =
                new com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest(notFoundToken);

        given(jwtTokenProvider.validateToken(notFoundToken))
                .willReturn(true);
        given(refreshTokenRepository.findByRefreshToken(notFoundToken))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissueToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.TOKEN_NOT_FOUND);

        verify(refreshTokenRepository, never()).delete(any());
    }
}