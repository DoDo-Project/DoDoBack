package com.dodo.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증(Auth) 도메인 관련 요청 데이터를 그룹화하여 관리하는 클래스입니다.
 * <p>
 * 소셜 로그인 인가 코드 전달, 토큰 재발급 요청 등 인증 프로세스 전반에서 클라이언트가 서버로 전송하는 데이터를 정의합니다.
 */
@Schema(description = "인증 관련 요청 DTO 그룹")
public class AuthRequest {

    /**
     * 소셜 로그인 인증을 위해 클라이언트가 전달하는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "소셜 로그인 및 토큰 발급 요청")
    public static class SocialLoginRequest {

        @Schema(description = "소셜 제공자 타입 (GOOGLE, NAVER)", example = "GOOGLE")
        @NotBlank(message = "provider는 필수 값입니다.")
        private String provider;

        @Schema(description = "OAuth 인가 코드 (Authorization Code)", example = "4/0AdQtV5...")
        @NotBlank(message = "code는 필수 값입니다.")
        private String code;
    }
}