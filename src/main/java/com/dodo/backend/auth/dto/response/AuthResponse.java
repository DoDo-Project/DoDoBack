package com.dodo.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class AuthResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SocialLoginResponse {

        @Schema(description = "응답 메시지", example = "로그인이 완료되었습니다.")
        private String message;

        @Schema(description = "프로필 이미지 URL", example = "https://i.pravatar.cc/150?img=3")
        private String profileUrl;

        @Schema(description = "서버 접근용 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
        private String accessToken;

        @Schema(description = "엑세스 토큰 재발급용 리프레시 토큰", example = "def50200f29184b29427741829283912948238138139128481391248124181284184128418412841284189381391283912839128391289481294812948129481283912839")
        private String refreshToken;

        @Schema(description = "액세스 토큰 만료 시간 (초 단위)", example = "3600")
        private Long accessTokenExpiresIn;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SocialRegisterResponse {

        @Schema(description = "응답 메시지", example = "추가 정보가 필요합니다.")
        private String message;

        @Schema(description = "소셜에서 가져온 이메일 (회원가입 시 사용)", example = "user@naver.com")
        private String email;

        @Schema(description = "소셜에서 가져온 이름", example = "홍길동")
        private String name;

        @Schema(description = "회원가입 요청 시 본인 인증용 토큰", example = "def50200f29184b29427741829283912948238138139128481391248124181284184128418412841284189381391283912839128391289481294812948129481283912839")
        private String registrationToken;

        @Schema(description = "액세스 토큰 만료 시간 (초 단위)", example = "3600")
        private Long tokenExpiresIn;
    }
}
