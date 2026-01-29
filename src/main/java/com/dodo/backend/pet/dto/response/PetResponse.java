package com.dodo.backend.pet.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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

    /**
     * 반려동물 정보 수정(Update)이 완료된 후, 변경된 최신 정보를 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 정보 수정 결과 응답")
    public static class PetUpdateResponse {

        @Schema(description = "응답 메시지", example = "새 반려동물을 등록완료했습니다.")
        private String message;

        @Schema(description = "반려동물 고유 ID", example = "1")
        private Long petId;

        @Schema(description = "변경된 등록번호", example = "123456789012345", nullable = true)
        private String registrationNumber;

        @Schema(description = "변경된 성별 (MALE, FEMALE, NEUTER)", example = "MALE")
        private String sex;

        @Schema(description = "변경된 나이", example = "4")
        private Integer age;

        @Schema(description = "생년월일", example = "2022-01-01T00:00:00")
        private LocalDateTime birth;

        @Schema(description = "변경된 펫 이름", example = "초코")
        private String petName;

        @Schema(description = "종 (CANINE, FELINE)", example = "CANINE")
        private String species;

        @Schema(description = "변경된 품종", example = "푸들")
        private String breed;

        @Schema(description = "변경된 심박수 기준치", example = "85")
        private Integer referenceHeartRate;

        @Schema(description = "변경된 디바이스 ID", example = "NEW-DEV-999")
        private String deviceId;

        /**
         * 수정된 필드값들을 직접 전달받아 수정 완료 응답 DTO로 변환합니다.
         *
         * @param petId              수정된 반려동물 고유 식별자
         * @param message            응답 메시지
         * @param registrationNumber 변경된 등록번호
         * @param sex                변경된 성별 (String)
         * @param age                변경된 나이
         * @param petName            변경된 이름
         * @param breed              변경된 품종
         * @param referenceHeartRate 변경된 심박수 기준
         * @param deviceId           변경된 디바이스 ID
         * @return 수정 완료 정보가 담긴 PetDetailResponse DTO
         */
        public static PetUpdateResponse toDto(Long petId, String message, String registrationNumber,
                                              String sex, Integer age, String petName,
                                              String breed, Integer referenceHeartRate, String deviceId) {
            return PetUpdateResponse.builder()
                    .petId(petId)
                    .message(message)
                    .registrationNumber(registrationNumber)
                    .sex(sex)
                    .age(age)
                    .petName(petName)
                    .breed(breed)
                    .referenceHeartRate(referenceHeartRate)
                    .deviceId(deviceId)
                    .build();
        }
    }

    /**
     * 반려동물 가족 초대를 위한 코드 발급 요청 시 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 가족 초대 코드 응답")
    public static class PetInvitationResponse {

        @Schema(description = "생성된 가족 초대 코드 (영문 대문자 + 숫자, 총 6자리)", example = "7X9K2P")
        private String code;

        @Schema(description = "초대 코드 유효 시간 (초 단위)", example = "900")
        private Long expiresIn;
    }
}