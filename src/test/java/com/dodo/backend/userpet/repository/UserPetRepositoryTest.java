package com.dodo.backend.userpet.repository;

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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UserPetRepository}의 데이터 접근 로직(JPA)을 검증하는 테스트 클래스입니다.
 * <p>
 * 실제 DB(또는 H2)와 연결하여 복합키(UserPetId)를 사용하는 엔티티의 저장(Save) 및 조회(Find) 기능이
 * 정상적으로 동작하는지 통합 테스트(SpringBootTest)를 통해 확인합니다.
 */
@SpringBootTest
@Transactional
@Slf4j
class UserPetRepositoryTest {

    @Autowired
    UserPetRepository userPetRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PetRepository petRepository;

    /**
     * UserPet 엔티티가 데이터베이스에 정상적으로 Insert 되는지 검증합니다.
     * <p>
     * User와 Pet을 먼저 저장한 후, 이들을 참조하는 UserPet 엔티티를 저장하고
     * 복합키(UserPetId) 및 필드 값들이 정확하게 유지되는지 확인합니다.
     */
    @Test
    @DisplayName("UserPet 저장 성공 테스트")
    void saveUserPetTest() {

        // given
        User user = User.builder()
                .email("test_save@dodo.com")
                .name("테스터")
                .nickname("저장테스트닉네임")
                .role(UserRole.USER)
                .userStatus(UserStatus.ACTIVE)
                .notificationEnabled(true)
                .profileUrl("https://test.com/profile.jpg")
                .region("서울")
                .build();
        userRepository.save(user);

        Pet pet = Pet.builder()
                .registrationNumber("REG-SAVE-001")
                .petName("바둑이")
                .species(PetSpecies.CANINE)
                .sex(PetSex.MALE)
                .age(3)
                .birth(LocalDateTime.now())
                .breed("말티즈")
                .deviceId("device_save_001")
                .build();
        petRepository.save(pet);

        UserPetId id = new UserPetId(user.getUsersId(), pet.getPetId());
        UserPet userPet = UserPet.builder()
                .id(id)
                .user(user)
                .pet(pet)
                .registrationStatus(RegistrationStatus.APPROVED)
                .build();

        // when
        UserPet savedUserPet = userPetRepository.save(userPet);

        log.info("저장된 UserPet ID: {}", savedUserPet.getId());
        log.info("저장된 상태: {}", savedUserPet.getRegistrationStatus());

        // then
        assertThat(savedUserPet).isNotNull();
        assertThat(savedUserPet.getId()).isEqualTo(id);
        assertThat(savedUserPet.getUser().getUsersId()).isEqualTo(user.getUsersId());
        assertThat(savedUserPet.getPet().getPetId()).isEqualTo(pet.getPetId());
        assertThat(savedUserPet.getRegistrationStatus()).isEqualTo(RegistrationStatus.APPROVED);
    }

    /**
     * 복합키(UserPetId)를 기준으로 UserPet 엔티티가 정확히 조회되는지 검증합니다.
     * <p>
     * 저장된 엔티티를 식별자로 조회했을 때 값이 존재해야 하며(Optional.isPresent),
     * 조회된 엔티티의 내부 필드 값이 저장된 값과 일치하는지 확인합니다.
     */
    @Test
    @DisplayName("UserPetId(복합키) 조회 테스트")
    void findByIdTest() {

        // given
        User user = User.builder()
                .email("test_find@dodo.com")
                .name("조회테스터")
                .nickname("조회테스트닉네임")
                .role(UserRole.USER)
                .userStatus(UserStatus.ACTIVE)
                .notificationEnabled(true)
                .profileUrl("https://test.com/profile.jpg")
                .region("부산")
                .build();
        userRepository.save(user);

        Pet pet = Pet.builder()
                .registrationNumber("REG-FIND-001")
                .petName("나비")
                .species(PetSpecies.FELINE)
                .sex(PetSex.FEMALE)
                .age(2)
                .birth(LocalDateTime.now())
                .breed("코리안숏헤어")
                .deviceId("device_find_001")
                .build();
        petRepository.save(pet);

        UserPetId id = new UserPetId(user.getUsersId(), pet.getPetId());
        UserPet userPet = UserPet.builder()
                .id(id)
                .user(user)
                .pet(pet)
                .registrationStatus(RegistrationStatus.APPROVED)
                .build();
        userPetRepository.save(userPet);

        // when
        Optional<UserPet> foundUserPet = userPetRepository.findById(id);

        log.info("조회된 UserPet 존재 여부: {}", foundUserPet.isPresent());
        if (foundUserPet.isPresent()) {
            log.info("조회된 User ID: {}", foundUserPet.get().getUser().getUsersId());
            log.info("조회된 Pet ID: {}", foundUserPet.get().getPet().getPetId());
        }

        // then
        assertThat(foundUserPet).isPresent();
        assertThat(foundUserPet.get().getUser().getUsersId()).isEqualTo(user.getUsersId());
        assertThat(foundUserPet.get().getPet().getPetId()).isEqualTo(pet.getPetId());
        assertThat(foundUserPet.get().getRegistrationStatus()).isEqualTo(RegistrationStatus.APPROVED);
    }
}