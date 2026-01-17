package com.dodo.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 애플리케이션의 보안 관련 설정을 담당하는 구성 클래스입니다.
 * <p>
 * Spring Security를 활성화하며, Scalar UI 접근 권한 및 API 엔드포인트에 대한
 * 보안 필터 체인을 정의합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * HTTP 요청에 대한 보안 필터 체인을 구성합니다.
     *
     * @param http HttpSecurity 객체를 통해 보안 설정을 구성합니다.
     * @return 구성된 SecurityFilterChain 인스턴스를 반환합니다.
     * @throws Exception 보안 구성 과정에서 발생할 수 있는 예외입니다.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
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
                );

        return http.build();
    }
}