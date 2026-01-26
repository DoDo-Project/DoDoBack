package com.dodo.backend.common.config;

import com.dodo.backend.common.jwt.JwtAuthenticationFilter;
import com.dodo.backend.common.jwt.JwtTokenProvider;
import com.dodo.backend.user.exception.UserErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 애플리케이션의 보안 관련 설정을 담당하는 구성 클래스입니다.
 * <p>
 * Spring Security를 활성화하며, JWT 인증 필터 등록 및 세션 정책(Stateless)을 설정합니다.
 * 필터 단계에서 발생하는 예외를 {@link com.dodo.backend.common.exception.ErrorResponse} 포맷에 맞춰 처리합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    /**
     * HTTP 요청에 대한 보안 필터 체인을 구성합니다.
     *
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain 인스턴스
     * @throws Exception 보안 구성 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/scalar/**",
                                "/auth/social-login",
                                "/view/login",
                                "/google-login",
                                "/naver-login"
                        ).permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).denyAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * 인증되지 않은 사용자가 보호된 리소스에 접근했을 때 401 에러를 반환하는 엔드포인트를 정의합니다.
     *
     * @return AuthenticationEntryPoint 인스턴스
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                sendErrorResponse(response, UserErrorCode.LOGIN_REQUIRED);
    }

    /**
     * 인가 권한이 없는 사용자가 보호된 리소스에 접근했을 때 403 에러를 반환하는 핸들러를 정의합니다.
     *
     * @return AccessDeniedHandler 인스턴스
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                sendErrorResponse(response, UserErrorCode.ACCESS_DENIED);
    }

    /**
     * ErrorResponse 포맷에 맞춰 클라이언트에게 JSON 응답을 전송합니다.
     *
     * @param response HTTP 응답 객체
     * @param errorCode 전송할 유저 에러 코드
     * @throws IOException 입출력 예외
     */
    private void sendErrorResponse(HttpServletResponse response, UserErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("status", errorCode.getHttpStatus().value());
        errorBody.put("message", errorCode.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}