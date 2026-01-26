package com.dodo.backend.user.dto.request;

import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자(User) 도메인 관련 요청 데이터를 그룹화하여 관리하는 클래스입니다.
 * <p>
 * 회원가입 시의 프로필 추가 정보 입력, 계정 상태 변경을 위한 본인 인증 번호 전달 등 사용자 정보 수정과 관련된 요청 구조를 정의합니다.
 */
@Schema(description = "유저 관련 요청 DTO 그룹")
public class UserRequest {

    /**
     * 회원가입(추가 정보 입력) 요청 시 클라이언트가 전송하는 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "회원가입 추가 정보 입력 요청")
    public static class UserRegisterRequest {

        @Schema(description = "유저 닉네임", example = "강력한 개발자")
        @NotBlank(message = "닉네임은 필수입니다.")
        private String nickname;

        @Schema(description = "활동 지역", example = "서울시 동대문구")
        @NotBlank(message = "지역 정보는 필수입니다.")
        private String region;

        @Schema(description = "가족 여부", example = "true")
        @NotNull(message = "가족 여부는 필수입니다.")
        private Boolean hasFamily;

        /**
         * DTO의 데이터와 식별자(Email)를 조합하여 업데이트용 User 엔티티를 생성합니다.
         */
        public User toEntity(String email) {
            return User.builder()
                    .email(email)
                    .nickname(this.nickname)
                    .region(this.region)
                    .hasFamily(this.hasFamily)
                    .build();
        }
    }

    /**
     * 회원 탈퇴 확정을 위한 인증 번호 검증 요청 DTO입니다.
     */
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "회원 탈퇴 본인 인증 요청")
    public static class WithdrawalRequest {

        @Schema(description = "이메일로 발송된 6자리 인증 번호", example = "936514")
        @NotBlank(message = "인증 번호는 필수입니다.")
        @Size(min = 6, max = 6, message = "인증 번호는 6자리여야 합니다.")
        private String authCode;
    }
}