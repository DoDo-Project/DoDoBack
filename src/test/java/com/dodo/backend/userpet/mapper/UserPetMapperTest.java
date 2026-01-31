package com.dodo.backend.userpet.mapper;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.entity.PetSex;
import com.dodo.backend.pet.entity.PetSpecies;
import com.dodo.backend.pet.repository.PetRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import com.dodo.backend.userpet.repository.UserPetRepository;
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
 * MyBatis 매퍼인 {@link UserPetMapper}의 SQL 실행 결과를 검증하는 통합 테스트 클래스입니다.
 * <p>
 * MyBatis와 JPA가 동일한 데이터베이스 세션을 공유하므로,
 * MyBatis를 통한 데이터 변경 후 영속성 컨텍스트(1차 캐시)를 초기화하여
 * 정합성을 검증합니다.
 */
@SpringBootTest
@Transactional
@Slf4j
class UserPetMapperTest {

    @Autowired
    private UserPetMapper userPetMapper;

    @Autowired
    private UserPetRepository userPetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private EntityManager em;

    private User testUser;
    private Pet testPet;
    private UserPet testUserPet;

    /**
     * 테스트 수행 전 기초 데이터를 생성합니다.
     * <p>
     * 유저와 펫을 생성하고, 둘 사이의 관계를 대기(PENDING) 상태로 초기화하여 저장합니다.
     * 이후 영속성 컨텍스트를 반영(flush) 및 초기화(clear)하여 순수 DB 상태와 동기화합니다.
     */
    @BeforeEach
    void setUp() {

        testUser = User.builder()
                .email("pet_mapper_test@example.com")
                .name("펫매퍼테스터")
                .nickname("집사")
                .region("서울")
                .hasFamily(true)
                .userStatus(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .notificationEnabled(true)
                .userCreatedAt(LocalDateTime.now())
                .profileUrl("https://example.com/default_profile.jpg")
                .build();
        userRepository.save(testUser);

        testPet = Pet.builder()
                .petName("테스트멍")
                .species(PetSpecies.CANINE)
                .breed("말티즈")
                .age(2)
                .sex(PetSex.MALE)
                .birth(LocalDateTime.now())
                .registrationNumber("9999999999")
                .deviceId("TEST_DEV_001")
                .build();
        petRepository.save(testPet);

        UserPetId id = new UserPetId(testUser.getUsersId(), testPet.getPetId());
        testUserPet = UserPet.builder()
                .id(id)
                .user(testUser)
                .pet(testPet)
                .registrationStatus(RegistrationStatus.PENDING)
                .build();
        userPetRepository.save(testUserPet);

        em.flush();
        em.clear();
        log.info("기초 데이터(User, Pet, UserPet-PENDING) 저장 및 영속성 컨텍스트 초기화 수행 완료");
    }

    /**
     * 매퍼를 통해 가족 등록 상태를 'APPROVED'로 변경하는 SQL이 정상적으로 실행되는지 검증합니다.
     */
    @Test
    @DisplayName("매퍼를 통한 가족 등록 상태 변경 (PENDING -> APPROVED)")
    void updateRegistrationStatusTest() {
        // given
        UserPetId targetId = new UserPetId(testUser.getUsersId(), testPet.getPetId());
        String targetStatus = "APPROVED";

        log.info("가족 상태 변경 매퍼 테스트 시작 - User: {}, Pet: {}, 변경할 상태: {}",
                testUser.getUsersId(), testPet.getPetId(), targetStatus);

        // when
        userPetMapper.updateRegistrationStatus(
                testUser.getUsersId(),
                testPet.getPetId(),
                targetStatus
        );

        // then
        em.clear();
        UserPet updatedUserPet = userPetRepository.findById(targetId).orElseThrow();

        assertThat(updatedUserPet.getRegistrationStatus()).isEqualTo(RegistrationStatus.APPROVED);

        log.info("가족 상태 변경 SQL 실행 결과 검증 성공");
    }

    /**
     * 매퍼를 통해 가족 등록 상태를 'REJECTED'로 변경하는 SQL이 정상적으로 실행되는지 검증합니다.
     */
    @Test
    @DisplayName("매퍼를 통한 가족 등록 상태 변경 (PENDING -> REJECTED)")
    void updateRegistrationStatusToRejectTest() {
        // given
        UserPetId targetId = new UserPetId(testUser.getUsersId(), testPet.getPetId());
        String targetStatus = "REJECTED";

        log.info("가족 상태 변경 매퍼 테스트 시작 - 변경할 상태: {}", targetStatus);

        // when
        userPetMapper.updateRegistrationStatus(
                testUser.getUsersId(),
                testPet.getPetId(),
                targetStatus
        );

        // then
        em.clear();
        UserPet updatedUserPet = userPetRepository.findById(targetId).orElseThrow();

        assertThat(updatedUserPet.getRegistrationStatus()).isEqualTo(RegistrationStatus.REJECTED);

        log.info("가족 거절 상태 변경 SQL 실행 결과 검증 성공");
    }
}