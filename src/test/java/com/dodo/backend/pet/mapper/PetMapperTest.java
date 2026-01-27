package com.dodo.backend.pet.mapper;

import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.entity.PetSex;
import com.dodo.backend.pet.entity.PetSpecies;
import com.dodo.backend.pet.repository.PetRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MyBatis 매퍼 {@link PetMapper}의 SQL 실행 결과를 검증하는 통합 테스트 클래스입니다.
 * <p>
 * MyBatis와 JPA가 동일한 DB 세션을 공유하므로, 영속성 컨텍스트(EntityManager)를 명시적으로
 * 관리하여 동적 SQL 업데이트 결과가 실제 데이터베이스에 정확히 반영되는지 검증합니다.
 */
@SpringBootTest
@Transactional
@Slf4j
class PetMapperTest {

    @Autowired
    private PetMapper petMapper;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private EntityManager em;

    private Pet testPet;

    /**
     * 테스트 수행을 위한 기초 반려동물 데이터를 생성하고 물리적 저장을 수행합니다.
     * <p>
     * age, birth, breed, deviceId 등 모든 NOT NULL 제약 조건을 충족하도록 구성하며,
     * 영속성 컨텍스트를 비워 MyBatis의 직접 수정을 감지할 수 있는 상태를 만듭니다.
     */
    @BeforeEach
    void setUp() {
        testPet = Pet.builder()
                .registrationNumber("REG-12345")
                .petName("초기바둑이")
                .species(PetSpecies.CANINE)
                .sex(PetSex.MALE)
                .age(3)
                .birth(LocalDateTime.now())
                .breed("진돗개")
                .deviceId("DODO-DEVICE-001")
                .referenceHeartRate(80)
                .build();

        petRepository.save(testPet);
        em.flush();
        em.clear();
        log.info("기초 반려동물 데이터 세팅 완료 (ID: {})", testPet.getPetId());
    }

    /**
     * MyBatis의 {@code <set>} 태그를 활용한 동적 쿼리가 수정 요청된 필드만 정확히 변경하는지 검증합니다.
     * <p>
     * 수정되지 않은 나머지 필드는 기존 데이터가 보존되어야 하며,
     * 변경 후 영속성 컨텍스트 초기화를 통해 DB 최신 값을 다시 로드하여 확인합니다.
     */
    @Test
    @DisplayName("매퍼를 통한 반려동물 프로필 선택적 수정 테스트")
    void updatePetProfileInfoTest() {
        // given
        Long petId = testPet.getPetId();
        PetRequest.PetUpdateRequest request = PetRequest.PetUpdateRequest.builder()
                .petName("수정된바둑이")
                .sex("FEMALE")
                .build();

        // when
        petMapper.updatePetProfileInfo(request, petId);

        // then
        em.clear();
        Pet result = petRepository.findById(petId).orElseThrow();

        assertThat(result.getPetName()).isEqualTo("수정된바둑이");
        assertThat(result.getSex()).isEqualTo(PetSex.FEMALE);
        assertThat(result.getDeviceId()).isEqualTo("DODO-DEVICE-001");
        assertThat(result.getBreed()).isEqualTo("진돗개");

        log.info("PetMapper 동적 필드 수정 검증 성공");
    }
}