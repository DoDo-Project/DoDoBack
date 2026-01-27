package com.dodo.backend.userpet.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.repository.UserPetRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

/**
 * {@link UserPetService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 * <p>
 * 유저와 펫 사이의 멤버십 관계(UserPet)가 올바르게 생성되고
 * 저장소로 전달되는지 단위 테스트를 통해 확인합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class UserPetServiceTest {

    @InjectMocks
    private UserPetServiceImpl userPetService;

    @Mock
    private UserPetRepository userPetRepository;

    /**
     * UserPet 관계 등록 성공 시나리오를 테스트합니다.
     * <p>
     * 전달받은 User, Pet 엔티티와 등록 상태(APPROVED)가
     * 올바르게 매핑되어 Repository에 저장되는지 검증합니다.
     */
    @Test
    @DisplayName("UserPet 등록 성공: 유저와 펫의 관계 엔티티가 올바르게 생성되어 저장된다.")
    void registerUserPet_Success() {
        log.info("테스트 시작: UserPet 등록 성공 시나리오");

        // given
        UUID userId = UUID.randomUUID();
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "usersId", userId);

        Pet pet = Pet.builder().build();
        ReflectionTestUtils.setField(pet, "petId", 100L);

        RegistrationStatus status = RegistrationStatus.APPROVED;

        // when
        userPetService.registerUserPet(user, pet, status);

        // then
        ArgumentCaptor<UserPet> captor = ArgumentCaptor.forClass(UserPet.class);
        verify(userPetRepository).save(captor.capture());

        UserPet savedUserPet = captor.getValue();
        log.info("저장된 UserPet 식별자 - 유저ID: {}, 펫ID: {}",
                savedUserPet.getId().getUserId(), savedUserPet.getId().getPetId());

        assertEquals(user, savedUserPet.getUser());
        assertEquals(pet, savedUserPet.getPet());
        assertEquals(status, savedUserPet.getRegistrationStatus());
        assertNotNull(savedUserPet.getId());

        log.info("테스트 종료: UserPet 등록 성공 시나리오");
    }
}