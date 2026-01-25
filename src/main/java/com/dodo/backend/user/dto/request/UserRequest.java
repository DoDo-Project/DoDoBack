package com.dodo.backend.user.dto.request;

import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저(User) 도메인의 요청 데이터를 그룹화하여 관리하는 클래스입니다.
 */
public class UserRequest {

    /**
     * 회원가입(추가 정보 입력) 요청 시 클라이언트가 전송하는 DTO입니다.
     * <p>
     * 사용 프로세스:
     * 1. 소셜 로그인 후 'REGISTER' 상태인 유저가 닉네임, 지역 등 필수 정보를 입력
     * 2. 이 DTO를 통해 전달된 데이터로 DB의 유저 정보를 업데이트하고 정회원(ACTIVE)으로 전환
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
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
         * <p>
         * MyBatis Mapper에 파라미터로 전달하기 위해 사용되며,
         * 이때 반환된 User 객체는 DB 저장이 아닌 업데이트 쿼리의 파라미터 용도로만 쓰입니다.
         *
         * @param email 토큰에서 추출한 유저의 이메일 (WHERE 조건)
         * @return 업데이트할 정보를 담은 User 객체
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
}