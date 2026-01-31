package com.dodo.backend.pet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 펫 도메인과 관련된 응답 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 * <p>
 * 펫 등록, 수정, 조회, 가족 초대 및 승인 등 다양한 API 응답에 사용되는
 * Inner Static Class들을 포함하고 있습니다.
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
         * 펫 ID와 메시지를 사용하여 응답 DTO 객체를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param petId   생성된 펫의 고유 식별자
         * @param message 클라이언트에게 전달할 성공 메시지
         * @return 초기화된 {@link PetRegisterResponse} 객체
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

        @Schema(description = "응답 메시지", example = "반려동물 정보 수정을 완료했습니다.")
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
         * @return 수정 완료 정보가 담긴 {@link PetUpdateResponse} DTO
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

    /**
     * 가족 초대 코드를 통해 등록 신청을 완료했을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "가족 초대 신청 결과 응답")
    public static class PetFamilyJoinRequestResponse {

        @Schema(description = "신청한 반려동물 ID", example = "101")
        private Long petId;

        @Schema(description = "처리 결과 메시지", example = "가족 등록을 신청했습니다. 승인을 기다려주세요.")
        private String message;

        /**
         * 펫 ID와 메시지를 받아 응답 DTO를 생성합니다.
         *
         * @param petId   신청된 반려동물 ID
         * @param message 처리 결과 메시지
         * @return 초기화된 {@link PetFamilyJoinRequestResponse} 객체
         */
        public static PetFamilyJoinRequestResponse toDto(Long petId, String message) {
            return PetFamilyJoinRequestResponse.builder()
                    .petId(petId)
                    .message(message)
                    .build();
        }
    }

    /**
     * 반려동물 목록 조회 시 반환되는 페이징 된 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 목록 조회 응답 (페이징 포함)")
    public static class PetListResponse {

        @Schema(description = "반려동물 데이터 목록")
        private List<PetSummary> pets;

        @Schema(description = "총 페이지 수", example = "5")
        private int totalPages;

        @Schema(description = "총 데이터 수", example = "48")
        private long totalElements;

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        /**
         * 서비스 계층에서 변환된 {@code Page<PetSummary>} 객체를 받아 최종 응답 DTO를 생성합니다.
         * <p>
         * 이 메소드는 엔티티를 직접 참조하지 않으며, 이미 DTO로 변환된 데이터만을 다룹니다.
         *
         * @param petPage 반려동물 요약 정보(DTO)가 담긴 Page 객체
         * @return 페이징 정보와 데이터가 포함된 {@link PetListResponse}
         */
        public static PetListResponse toDto(Page<PetSummary> petPage) {
            return PetListResponse.builder()
                    .pets(petPage.getContent())
                    .totalPages(petPage.getTotalPages())
                    .totalElements(petPage.getTotalElements())
                    .currentPage(petPage.getNumber())
                    .pageSize(petPage.getSize())
                    .build();
        }

        /**
         * 목록 내 개별 반려동물 요약 정보 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "반려동물 요약 정보")
        public static class PetSummary {
            @Schema(description = "반려동물 ID", example = "101")
            private Long petId;

            @Schema(description = "이름", example = "보리")
            private String petName;

            @Schema(description = "이미지 URL", example = "https://example.com/images/bori.jpg")
            private String imageFileUrl;

            @Schema(description = "종 (CANINE, FELINE)", example = "CANINE")
            private String species;

            @Schema(description = "품종", example = "말티즈")
            private String breed;

            @Schema(description = "성별", example = "FEMALE")
            private String sex;

            @Schema(description = "나이", example = "5")
            private Integer age;

            @Schema(description = "생년월일", example = "2020-09-30")
            private LocalDateTime birth;

            @Schema(description = "체중", example = "4.2")
            private Double weight;

            @Schema(description = "등록번호", example = "4102020001231")
            private String registrationNumber;
        }
    }

    /**
     * 가족 승인/거절 처리가 완료되었을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "가족 승인/거절 처리 결과 응답")
    public static class PetFamilyApprovalResponse {

        @Schema(description = "대상 반려동물 ID", example = "101")
        private Long petId;

        @Schema(description = "처리 결과 메시지", example = "가족 신청을 승인했습니다.")
        private String message;

        /**
         * 펫 ID와 처리 메시지를 받아 응답 DTO를 생성합니다.
         *
         * @param petId   처리된 반려동물 ID
         * @param message 처리 결과 메시지 (승인/거절 여부 포함)
         * @return 초기화된 {@link PetFamilyApprovalResponse} 객체
         */
        public static PetFamilyApprovalResponse toDto(Long petId, String message) {
            return PetFamilyApprovalResponse.builder()
                    .petId(petId)
                    .message(message)
                    .build();
        }
    }
}