package com.dodo.backend.common.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 요청의 헤더에서 JWT 토큰을 추출하고 인증을 수행하는 커스텀 필터입니다.
 * <p>
 * 매 요청마다 Authorization 헤더를 검사하여 토큰의 유효성을 확인하고,
 * 유효한 경우 SecurityContext에 인증 객체를 저장하여 지속적인 보안 컨텍스트를 유지합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 필터 체인 내에서 실제 인증 로직을 수행하는 메서드입니다.
     * <p>
     * 요청 헤더에서 토큰을 추출하고 유효성을 검증한 뒤, 정상적인 토큰일 경우
     * SecurityContextHolder에 인증 정보를 설정하여 이후 요청 처리 과정에서 유저 정보를 사용할 수 있게 합니다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security Context에 '{}' 인증 정보를 저장했습니다", authentication.getName());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 JWT 토큰 문자열을 추출합니다.
     * <p>
     * 'Authorization' 헤더가 존재하고 'Bearer '로 시작하는 경우,
     * 접두사를 제외한 순수 토큰 값만을 반환합니다.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}