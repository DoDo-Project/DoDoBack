package com.dodo.backend.user.dto.response;

import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 유저(User) 도메인의 응답 데이터를 그룹화하여 관리하는 클래스입니다.
 */
public class UserResponse {

    /**
     * 회원가입 완료 후 클라이언트에게 반환되는 응답 DTO입니다.
     * <p>
     * 사용 프로세스:
     * 1. 추가 정보 입력 및 DB 업데이트(MyBatis) 성공
     * 2. 서비스 이용이 가능한 정식 Access/Refresh Token 발급
     * 3. 클라이언트는 이 토큰을 저장하여 로그인 상태 유지
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class UserRegisterResponse {

        @Schema(description = "응답 메시지", example = "회원가입이 완료되었습니다.")
        private String message;

        @Schema(description = "프로필 이미지 URL", example = "https://i.pravatar.cc/150?img=3")
        private String profileUrl;

        @Schema(description = "서버 접근용 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String accessToken;

        @Schema(description = "엑세스 토큰 재발급용 리프레시 토큰", example = "def50200f29184b294277418292...")
        private String refreshToken;

        @Schema(description = "액세스 토큰 만료 시간 (밀리초 단위)", example = "3600000")
        private Long accessTokenExpiresIn;

        /**
         * 정적 팩토리 메서드: User 엔티티와 발급된 토큰 정보를 받아 응답 DTO를 생성합니다.
         */
        public static UserRegisterResponse toDto(User user, String message, String accessToken, String refreshToken, Long accessTokenExpiresIn) {
            return UserRegisterResponse.builder()
                    .message(message)
                    .profileUrl(user.getProfileUrl())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(accessTokenExpiresIn)
                    .build();
        }
    }
}