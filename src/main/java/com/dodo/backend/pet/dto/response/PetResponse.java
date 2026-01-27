package com.dodo.backend.pet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 펫 도메인과 관련된 응답 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "펫 관련 응답 DTO 그룹")
public class PetResponse {

    /**
     * 펫 등록 처리가 성공적으로 완료되었을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "펫 등록 완료 응답")
    public static class PetRegisterResponse {

        @Schema(description = "생성된 펫 ID", example = "1")
        private Long petId;

        @Schema(description = "응답 메시지", example = "새 반려동물을 등록완료했습니다.")
        private String message;



        /**
         * 펫 ID를 사용하여 응답 DTO 객체를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param petId 생성된 펫의 고유 식별자
         * @return 초기화된 PetRegisterResponse 객체
         */
        public static PetRegisterResponse toDto(Long petId, String message) {
            return PetRegisterResponse.builder()
                    .petId(petId)
                    .message(message)
                    .build();
        }
    }
}