package com.dodo.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 인증(Auth) 도메인 관련 응답 데이터를 그룹화하여 관리하는 클래스입니다.
 */
public class AuthResponse {

    /**
     * 소셜 로그인 성공 시(기존 회원) 클라이언트에게 반환하는 응답 DTO입니다.
     * <p>
     * 사용 프로세스:
     * 1. 이메일 조회 결과 이미 가입된 정회원(ACTIVE)임이 확인됨
     * 2. 서비스 이용이 가능한 Access Token과 Refresh Token을 즉시 발급
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SocialLoginResponse {

        @Schema(description = "응답 메시지", example = "로그인이 완료되었습니다.")
        private String message;

        @Schema(description = "프로필 이미지 URL", example = "https://i.pravatar.cc/150?img=3")
        private String profileUrl;

        @Schema(description = "서버 접근용 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String accessToken;

        @Schema(description = "엑세스 토큰 재발급용 리프레시 토큰", example = "def50200f29184b294277418292...")
        private String refreshToken;

        @Schema(description = "액세스 토큰 만료 시간 (밀리초 단위)", example = "3600000")
        private Long accessTokenExpiresIn;

        public static SocialLoginResponse toDto(String message, String profileUrl, String accessToken, String refreshToken, Long accessTokenExpiresIn) {
            return SocialLoginResponse.builder()
                    .message(message)
                    .profileUrl(profileUrl)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(accessTokenExpiresIn)
                    .build();
        }
    }

    /**
     * 신규 회원가입 대상자일 경우 반환하는 응답 DTO입니다.
     * <p>
     * 사용 프로세스:
     * 1. 이메일 조회 결과 가입되지 않은 신규 유저임이 확인됨
     * 2. 추가 정보 입력을 위해 본인 인증용 임시 토큰(registrationToken) 발급
     * 3. 클라이언트는 이 토큰과 함께 닉네임 등을 입력하여 회원가입 완료 요청
     */
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

        @Schema(description = "회원가입 요청 시 본인 인증용 토큰", example = "def50200f29184b294277418292...")
        private String registrationToken;

        @Schema(description = "임시 토큰 만료 시간 (밀리초 단위)", example = "1800000")
        private Long tokenExpiresIn;

        public static SocialRegisterResponse toDto(String message, String email, String name, String registrationToken, Long tokenExpiresIn){
            return SocialRegisterResponse.builder()
                    .message(message)
                    .email(email)
                    .name(name)
                    .registrationToken(registrationToken)
                    .tokenExpiresIn(tokenExpiresIn)
                    .build();
        }
    }
}