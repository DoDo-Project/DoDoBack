package com.dodo.backend.common.config;

import com.dodo.backend.common.util.JwtAuthenticationFilter;
import com.dodo.backend.common.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 애플리케이션의 보안 관련 설정을 담당하는 구성 클래스입니다.
 * <p>
 * Spring Security를 활성화하며, JWT 인증 필터 등록 및 세션 정책(Stateless)을 설정합니다.
 * 또한 Scalar UI 및 API 엔드포인트에 대한 접근 권한을 관리합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * HTTP 요청에 대한 보안 필터 체인을 구성합니다.
     * <p>
     * CSRF 비활성화, 세션 생성 정책을 Stateless로 설정하며,
     * JWT 인증 필터를 UsernamePasswordAuthenticationFilter 이전에 배치하여 토큰 인증을 수행합니다.
     *
     * @param http HttpSecurity 객체를 통해 보안 설정을 구성합니다.
     * @return 구성된 SecurityFilterChain 인스턴스를 반환합니다.
     * @throws Exception 보안 구성 과정에서 발생할 수 있는 예외입니다.
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
                                "/scalar/**"
                        ).permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).denyAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}