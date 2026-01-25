package com.dodo.backend.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JwtTokenProvider}의 토큰 생성 및 검증 기능을 테스트하는 클래스입니다.
 */
@Slf4j
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "testSecretKeyForJwtTokenProviderTestUsingHmacShaKey";
    private final long accessTokenValidity = 3600000;
    private final long refreshTokenValidity = 86400000;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey, accessTokenValidity, refreshTokenValidity);
    }

    @Test
    @DisplayName("액세스 토큰 생성 및 정보 추출 테스트")
    void createAccessTokenTest() {
        // given
        UUID userId = UUID.randomUUID();
        String role = "USER";

        // when
        String token = jwtTokenProvider.createAccessToken(userId, role);

        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        Authentication auth = jwtTokenProvider.getAuthentication(token);
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

        log.info("Created Access Token: {}", token);
        log.info("Extracted UserId: {}", extractedUserId);
        log.info("Extracted Authorities: {}", authorities);

        // then
        assertThat(token).isNotNull();
        assertThat(extractedUserId).isEqualTo(userId);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + role);
    }

    @Test
    @DisplayName("회원가입용 임시 토큰 생성 및 이메일 추출 테스트")
    void createRegisterTokenTest() {
        // given
        String email = "test@dodo.com";

        // when
        String token = jwtTokenProvider.createRegisterToken(email);

        Authentication auth = jwtTokenProvider.getAuthentication(token);
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String extractedEmail = userDetails.getUsername();

        log.info("Created Register Token: {}", token);
        log.info("Extracted Email via UserDetails: {}", extractedEmail);
        log.info("Extracted Authorities: {}", auth.getAuthorities());

        // then
        assertThat(token).isNotNull();
        assertThat(extractedEmail).isEqualTo(email);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_GUEST");
    }

    @Test
    @DisplayName("잘못된 토큰 검증 테스트")
    void invalidTokenTest() {
        // given
        String invalidToken = "invalid.jwt.token";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);
        log.info("Invalid Token Validation Result: {}", isValid);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰 생성 테스트")
    void createRefreshTokenTest() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        String token = jwtTokenProvider.createRefreshToken(userId);
        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        log.info("Created Refresh Token: {}", token);
        log.info("Extracted UserId from Refresh Token: {}", extractedUserId);

        // then
        assertThat(token).isNotNull();
        assertThat(extractedUserId).isEqualTo(userId);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }
}