package com.dodo.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthRequest {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SocialLoginRequest {

        @Schema(description = "소셜 제공자 타입 (GOOGLE, NAVER, KAKAO 등)", example = "GOOGLE")
        @NotBlank(message = "provider는 필수 값입니다.")
        private String provider;

        @Schema(description = "OAuth 인가 코드 (Authorization Code)", example = "4/0AdQtV5...")
        @NotBlank(message = "code는 필수 값입니다.")
        private String code;
    }

}
