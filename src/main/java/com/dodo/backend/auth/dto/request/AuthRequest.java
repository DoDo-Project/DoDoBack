package com.dodo.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증(Auth) 도메인 관련 요청 데이터를 그룹화하여 관리하는 클래스입니다.
 */
public class AuthRequest {

    /**
     * 소셜 로그인 인증을 위해 클라이언트가 전달하는 요청 DTO입니다.
     * <p>
     * 사용 프로세스:
     * 1. 클라이언트가 소셜 인증 서버(Google/Naver)로부터 인가 코드(Code) 발급
     * 2. 발급된 코드와 제공자 타입(Provider)을 이 객체에 담아 백엔드에 토큰 발급 요청
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SocialLoginRequest {

        @Schema(description = "소셜 제공자 타입 (GOOGLE, NAVER)", example = "GOOGLE")
        @NotBlank(message = "provider는 필수 값입니다.")
        private String provider;

        @Schema(description = "OAuth 인가 코드 (Authorization Code)", example = "4/0AdQtV5...")
        @NotBlank(message = "code는 필수 값입니다.")
        private String code;
    }

}