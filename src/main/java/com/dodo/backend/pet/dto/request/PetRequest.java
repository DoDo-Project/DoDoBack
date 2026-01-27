package com.dodo.backend.pet.dto.request;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.entity.PetSex;
import com.dodo.backend.pet.entity.PetSpecies;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 펫 도메인과 관련된 요청 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "펫 관련 요청 DTO 그룹")
public class PetRequest {

    /**
     * 신규 펫 등록을 위해 클라이언트로부터 전달받는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "펫 등록 요청")
    public static class PetRegisterRequest {

        @Schema(description = "반려동물 등록번호 (선택사항, 없을 시 null)", example = "null", nullable = true)
        private String registrationNumber;

        @Schema(description = "성별 (MALE, FEMALE, NEUTER)", example = "MALE")
        @NotNull(message = "성별은 필수입니다.")
        private PetSex sex;

        @Schema(description = "나이", example = "3")
        @NotNull(message = "나이는 필수입니다.")
        private Integer age;

        @Schema(description = "생년월일 (ISO8601)", example = "2022-01-01T00:00:00")
        @NotNull(message = "생일은 필수입니다.")
        private LocalDateTime birth;

        @Schema(description = "펫 이름", example = "바둑이")
        @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
        @NotBlank(message = "이름은 필수입니다.")
        private String petName;

        @Schema(description = "종 (CANINE, FELINE)", example = "CANINE")
        @NotNull(message = "종은 필수입니다.")
        private PetSpecies species;

        @Schema(description = "품종", example = "진돗개")
        @NotBlank(message = "품종은 필수입니다.")
        private String breed;

        @Schema(description = "심박수 기준", example = "80")
        @NotNull(message = "심박수는 필수입니다.")
        @Positive(message = "심박수는 양수여야 합니다.")
        private Integer referenceHeartRate;

        @Schema(description = "디바이스 ID", example = "ABC123XYZ")
        @NotBlank(message = "디바이스 ID는 필수입니다.")
        private String deviceId;

        /**
         * 요청 DTO의 데이터를 기반으로 새로운 {@link Pet} 엔티티를 생성합니다.
         *
         * @return 초기화된 Pet 엔티티 객체
         */
        public Pet toEntity() {
            return Pet.builder()
                    .registrationNumber(this.registrationNumber)
                    .sex(this.sex)
                    .age(this.age)
                    .birth(this.birth)
                    .petName(this.petName)
                    .species(this.species)
                    .breed(this.breed)
                    .referenceHeartRate(this.referenceHeartRate)
                    .deviceId(this.deviceId)
                    .build();
        }
    }

    /**
     * 반려동물 정보 수정을 위한 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "반려동물 정보 수정 요청")
    public static class PetUpdateRequest {

        @Schema(description = "변경할 등록번호 (선택)", example = "159263487012345")
        @Size(max = 15, message = "등록번호는 15자 이하여야 합니다.")
        private String registrationNumber;

        @Schema(description = "변경할 성별 (선택, MALE, FEMALE, NEUTER)", example = "FEMALE")
        private String sex;

        @Schema(description = "변경할 나이 (선택)", example = "5")
        @Positive(message = "나이는 양수여야 합니다.")
        private Integer age;

        @Schema(description = "변경할 펫 이름 (선택)", example = "까미")
        @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
        private String petName;

        @Schema(description = "변경할 품종 (선택)", example = "리트리버")
        private String breed;

        @Schema(description = "변경할 심박수 기준치 (선택)", example = "90")
        @Positive(message = "심박수는 양수여야 합니다.")
        private Integer referenceHeartRate;

        @Schema(description = "변경할 디바이스 ID (선택)", example = "DEV-UPDATE-777")
        private String deviceId;

    }
}