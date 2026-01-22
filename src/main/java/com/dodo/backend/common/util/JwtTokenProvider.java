package com.dodo.backend.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 생성, 검증 및 정보 추출을 담당하는 컴포넌트입니다.
 * <p>
 * Access Token, Refresh Token 및 회원가입용 임시 토큰을 관리합니다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;
    private static final long REGISTER_TOKEN_VALIDITY = 1000 * 60 * 30;

    /**
     * 설정 파일의 프로퍼티를 주입받아 서명 키와 유효 시간을 초기화합니다.
     */
    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access-token-validity}") long accessTokenValidity,
                            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /**
     * 사용자 ID와 권한 정보를 기반으로 액세스 토큰을 생성합니다.
     */
    public String createAccessToken(UUID userId, String role) {
        return createToken(userId, role, accessTokenValidity);
    }

    /**
     * 사용자 ID를 기반으로 리프레시 토큰을 생성합니다.
     */
    public String createRefreshToken(UUID userId) {
        return createToken(userId, null, refreshTokenValidity);
    }

    /**
     * 회원가입 진행을 위해 이메일 정보를 담은 임시 토큰을 생성합니다.
     */
    public String createRegisterToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + REGISTER_TOKEN_VALIDITY);

        return Jwts.builder()
                .subject("register-process")
                .claim("email", email)
                .claim("role", "GUEST")
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    private String createToken(UUID userId, String role, long validity) {
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + validity);

        JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(validityDate)
                .signWith(key);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    /**
     * 토큰에서 사용자 ID(Subject)를 추출합니다.
     */
    public UUID getUserIdFromToken(String token) {
        String subject = parseClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    /**
     * 토큰에서 권한(Role) 정보를 추출합니다.
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 토큰에서 이메일 정보를 추출합니다.
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰의 유효성(서명, 만료 여부 등)을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}