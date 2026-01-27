package com.dodo.backend.pet.repository;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.entity.PetSex;
import com.dodo.backend.pet.entity.PetSpecies;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PetRepository}의 데이터 접근 로직(JPA)을 검증하는 테스트 클래스입니다.
 * <p>
 * 실제 DB(또는 H2)와 연결하여 엔티티의 저장(Save) 및 조회(Find) 기능이
 * 정상적으로 동작하는지 통합 테스트(SpringBootTest)를 통해 확인합니다.
 */
@SpringBootTest
@Transactional
@Slf4j
class PetRepositoryTest {

    @Autowired
    PetRepository petRepository;

    /**
     * 새로운 반려동물 엔티티가 데이터베이스에 정상적으로 Insert 되는지 검증합니다.
     * <p>
     * 저장 후 생성된 PK(ID)의 존재 여부와 저장된 등록번호를 확인합니다.
     */
    @Test
    @DisplayName("반려동물 저장 성공 테스트")
    void savePetTest() {

        // given
        Pet pet = Pet.builder()
                .registrationNumber("REG-12345")
                .petName("바둑이")
                .species(PetSpecies.CANINE)
                .sex(PetSex.MALE)
                .age(3)
                .birth(LocalDateTime.now())
                .breed("진돗개")
                .deviceId("device1")
                .build();

        // when
        Pet savedPet = petRepository.save(pet);

        log.info("저장된 반려동물 정보: {}", savedPet);
        log.info("저장된 반려동물 ID: {}", savedPet.getPetId());

        // then
        assertThat(savedPet.getPetId()).isNotNull();
        assertThat(savedPet.getRegistrationNumber()).isEqualTo("REG-12345");
        assertThat(savedPet.getPetName()).isEqualTo("바둑이");
    }

    /**
     * 등록번호(Unique Key)를 기준으로 반려동물 존재 여부를 정확히 확인하는지 검증합니다.
     */
    @Test
    @DisplayName("등록번호 존재 여부 확인 테스트")
    void existsByRegistrationNumberTest() {

        // given
        String regNum = "REG-12345";
        Pet pet = Pet.builder()
                .registrationNumber(regNum)
                .petName("바둑이")
                .species(PetSpecies.CANINE)
                .sex(PetSex.MALE)
                .age(3)
                .birth(LocalDateTime.now())
                .breed("진돗개")
                .deviceId("device2")
                .build();

        petRepository.save(pet);
        petRepository.flush();
        log.info("테스트용 등록번호 저장 완료: {}", regNum);

        // when
        boolean exists = petRepository.existsByRegistrationNumber(regNum);
        boolean notExists = petRepository.existsByRegistrationNumber("NON-EXIST-999");

        log.info("등록번호 '{}' 존재 여부: {}", regNum, exists);
        log.info("등록번호 'NON-EXIST-999' 존재 여부: {}", notExists);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    /**
     * 특정 반려동물 엔티티를 데이터베이스에서 삭제했을 때, 해당 데이터가 정상적으로 제거되는지 검증합니다.
     * <p>
     * 삭제 실행 후, 동일한 식별자(ID)로 조회 시 데이터가 존재하지 않음을 확인합니다.
     */
    @Test
    @DisplayName("반려동물 삭제 성공 테스트")
    void deletePetTest() {
        // given
        Pet pet = Pet.builder()
                .registrationNumber("DEL-777")
                .petName("삭제대상")
                .species(PetSpecies.FELINE)
                .sex(PetSex.FEMALE)
                .age(3)
                .birth(LocalDateTime.now())
                .breed("진돗개")
                .deviceId("device3")
                .build();
        Pet savedPet = petRepository.save(pet);
        Long targetId = savedPet.getPetId();

        log.info("삭제하기 전 - 반려동물 ID: {}, 등록번호: {}", targetId, savedPet.getRegistrationNumber());

        // when
        petRepository.delete(savedPet);
        petRepository.flush();

        // then
        boolean isExists = petRepository.existsById(targetId);
        java.util.Optional<Pet> foundPet = petRepository.findById(targetId);

        log.info("삭제한 후 - 타겟ID: {}", targetId);
        log.info("삭제한 후 - 존재 상태: {}", isExists);

        assertThat(isExists).isFalse();
        assertThat(foundPet).isEmpty();
    }
}