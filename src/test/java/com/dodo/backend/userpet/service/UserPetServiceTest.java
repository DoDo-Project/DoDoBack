package com.dodo.backend.userpet.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import com.dodo.backend.userpet.exception.UserPetErrorCode;
import com.dodo.backend.userpet.exception.UserPetException;
import com.dodo.backend.userpet.repository.UserPetRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link UserPetService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 * <p>
 * 유저와 펫 사이의 멤버십 관계(UserPet) 생성 및
 * 가족 초대 코드 발급/수락 로직을 단위 테스트로 확인합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class UserPetServiceTest {

    @InjectMocks
    private UserPetServiceImpl userPetService;

    @Mock
    private UserPetRepository userPetRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private UserService userService;

    @Mock
    private PetService petService;

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

        given(userService.getUserEntity(userId)).willReturn(user);

        // when
        userPetService.registerUserPet(user.getUsersId(), pet, status);

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

    /**
     * 가족 초대 코드 발급 성공 시나리오를 테스트합니다.
     * <p>
     * 1. 요청자가 해당 펫의 승인된(APPROVED) 멤버인지 확인합니다.
     * 2. Redis에 중복된 키가 없는지 확인합니다.
     * 3. 랜덤 코드가 생성되고 Redis에 2회(초대용, 중복방지용) 저장되는지 검증합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 성공: 승인된 멤버라면 코드가 생성되고 Redis에 저장된다.")
    void generateInvitationCode_Success() {
        log.info("테스트 시작: 초대 코드 발급 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 100L;
        UserPet userPet = UserPet.builder()
                .registrationStatus(RegistrationStatus.APPROVED)
                .build();

        given(userPetRepository.findById(any(UserPetId.class))).willReturn(Optional.of(userPet));
        given(redisTemplate.hasKey(anyString())).willReturn(false);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        Map<String, Object> result = userPetService.generateInvitationCode(userId, petId);

        // then
        log.info("생성된 초대 코드: {}", result.get("code"));
        assertNotNull(result.get("code"));
        assertNotNull(result.get("expiresIn"));

        verify(valueOperations, times(2)).set(anyString(), anyString(), any(Duration.class));

        log.info("테스트 종료: 초대 코드 발급 성공");
    }

    /**
     * 가족 구성원이 아닌 유저가 초대를 시도할 때 예외 발생을 테스트합니다.
     * <p>
     * {@link UserPetErrorCode#INVITE_PERMISSION_DENIED} 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 실패: 해당 펫의 멤버가 아니면 권한 없음 예외가 발생한다.")
    void generateInvitationCode_Fail_NotMember() {
        log.info("테스트 시작: 초대 코드 발급 실패 (멤버 아님)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 100L;

        given(userPetRepository.findById(any(UserPetId.class))).willReturn(Optional.empty());

        // when & then
        UserPetException exception = assertThrows(UserPetException.class, () ->
                userPetService.generateInvitationCode(userId, petId)
        );

        assertEquals(UserPetErrorCode.INVITE_PERMISSION_DENIED, exception.getErrorCode());
        log.info("테스트 종료: 초대 코드 발급 실패 (멤버 아님)");
    }

    /**
     * 승인되지 않은(PENDING) 멤버가 초대를 시도할 때 예외 발생을 테스트합니다.
     * <p>
     * {@link UserPetErrorCode#INVITE_PERMISSION_DENIED} 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 실패: 승인되지 않은 멤버는 권한 없음 예외가 발생한다.")
    void generateInvitationCode_Fail_NotApproved() {
        log.info("테스트 시작: 초대 코드 발급 실패 (미승인 상태)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 100L;
        UserPet userPet = UserPet.builder()
                .registrationStatus(RegistrationStatus.PENDING)
                .build();

        given(userPetRepository.findById(any(UserPetId.class))).willReturn(Optional.of(userPet));

        // when & then
        UserPetException exception = assertThrows(UserPetException.class, () ->
                userPetService.generateInvitationCode(userId, petId)
        );

        assertEquals(UserPetErrorCode.INVITE_PERMISSION_DENIED, exception.getErrorCode());
        log.info("테스트 종료: 초대 코드 발급 실패 (미승인 상태)");
    }

    /**
     * 이미 유효한 초대 코드가 존재하는 경우 예외 발생을 테스트합니다.
     * <p>
     * Redis에 해당 펫의 키가 존재할 때,
     * {@link UserPetErrorCode#INVITATION_ALREADY_EXISTS} 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 실패: 이미 유효한 코드가 존재하면 중복(Conflict) 예외가 발생한다.")
    void generateInvitationCode_Fail_AlreadyExists() {
        log.info("테스트 시작: 초대 코드 발급 실패 (중복 발급)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 100L;
        UserPet userPet = UserPet.builder()
                .registrationStatus(RegistrationStatus.APPROVED)
                .build();

        given(userPetRepository.findById(any(UserPetId.class))).willReturn(Optional.of(userPet));
        given(redisTemplate.hasKey(anyString())).willReturn(true);

        // when & then
        UserPetException exception = assertThrows(UserPetException.class, () ->
                userPetService.generateInvitationCode(userId, petId)
        );

        assertEquals(UserPetErrorCode.INVITATION_ALREADY_EXISTS, exception.getErrorCode());

        verify(redisTemplate, times(0)).opsForValue();

        log.info("테스트 종료: 초대 코드 발급 실패 (중복 발급)");
    }

    /**
     * 초대 코드 입력 및 가족 수락 성공 시나리오를 테스트합니다.
     * <p>
     * 1. Redis에서 코드로 petId를 조회합니다.
     * 2. 이미 가족인지 중복 검사를 수행합니다.
     * 3. PetService를 통해 펫 정보를 조회합니다.
     * 4. registerUserPet을 호출하여 저장하고, 최종 가족 목록을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("초대 수락 성공: 유효한 코드 입력 시 가족으로 등록되고 목록을 반환한다.")
    void joinFamilyByCode_Success() {
        log.info("테스트 시작: 초대 수락 성공");

        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        String petIdStr = "100";
        Long petId = 100L;

        Pet pet = Pet.builder().build();
        ReflectionTestUtils.setField(pet, "petId", petId);

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "usersId", userId);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(petIdStr);

        given(userPetRepository.existsById(any(UserPetId.class))).willReturn(false);
        given(petService.getPet(petId)).willReturn(pet);
        given(userService.getUserEntity(userId)).willReturn(user);
        given(userPetRepository.findAllByPetId(petId)).willReturn(List.of());

        // when
        Map<String, Object> result = userPetService.joinFamilyByCode(userId, invitationCode);

        // then
        assertNotNull(result);
        assertEquals(pet, result.get("pet"));
        assertNotNull(result.get("members"));

        verify(userPetRepository).save(any(UserPet.class));
        log.info("테스트 종료: 초대 수락 성공");
    }

    /**
     * 만료되거나 존재하지 않는 초대 코드를 입력했을 때 예외 발생을 테스트합니다.
     * <p>
     * Redis에서 null이 반환될 때 {@link UserPetErrorCode#INVITATION_NOT_FOUND}가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("초대 수락 실패: 코드가 존재하지 않거나 만료된 경우 예외가 발생한다.")
    void joinFamilyByCode_Fail_InvalidCode() {
        log.info("테스트 시작: 초대 수락 실패 (코드 없음)");

        // given
        UUID userId = UUID.randomUUID();
        String invalidCode = "INVALID";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);

        // when & then
        UserPetException exception = assertThrows(UserPetException.class, () ->
                userPetService.joinFamilyByCode(userId, invalidCode)
        );

        assertEquals(UserPetErrorCode.INVITATION_NOT_FOUND, exception.getErrorCode());
        log.info("테스트 종료: 초대 수락 실패 (코드 없음)");
    }

    /**
     * 이미 가족으로 등록된 사용자가 초대를 수락하려 할 때 예외 발생을 테스트합니다.
     * <p>
     * {@link UserPetErrorCode#ALREADY_FAMILY_MEMBER} 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("초대 수락 실패: 이미 가족 멤버인 경우 예외가 발생한다.")
    void joinFamilyByCode_Fail_AlreadyMember() {
        log.info("테스트 시작: 초대 수락 실패 (이미 멤버)");

        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        String petIdStr = "100";
        Long petId = 100L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(petIdStr);

        given(userPetRepository.existsById(any(UserPetId.class))).willReturn(true);

        // when & then
        UserPetException exception = assertThrows(UserPetException.class, () ->
                userPetService.joinFamilyByCode(userId, invitationCode)
        );

        assertEquals(UserPetErrorCode.ALREADY_FAMILY_MEMBER, exception.getErrorCode());

        verify(userPetRepository, times(0)).save(any(UserPet.class));

        log.info("테스트 종료: 초대 수락 실패 (이미 멤버)");
    }

    /**
     * 유저의 펫 목록 조회 시나리오를 테스트합니다.
     * <p>
     * Repository를 통해 조회된 페이징 결과({@code Page<UserPet>})가
     * 결과 Map에 "userPetPage"라는 키로 올바르게 담기는지 검증합니다.
     */
    @Test
    @DisplayName("유저 펫 목록 조회 성공: 페이징된 펫 목록이 Map에 담겨 반환된다.")
    void getUserPets_Success() {
        log.info("테스트 시작: 유저 펫 목록 조회 성공");

        // given
        UUID userId = UUID.randomUUID();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        
        List<UserPet> emptyList = java.util.Collections.emptyList();
        org.springframework.data.domain.Page<UserPet> mockPage =
                new org.springframework.data.domain.PageImpl<>(emptyList, pageable, 0);

        given(userPetRepository.findAllByUser_UsersId(userId, pageable)).willReturn(mockPage);

        // when
        Map<String, Object> result = userPetService.getUserPets(userId, pageable);

        // then
        log.info("조회된 결과 Map Key Set: {}", result.keySet());

        assertNotNull(result);
        assertTrue(result.containsKey("userPetPage"));
        assertEquals(mockPage, result.get("userPetPage"));

        verify(userPetRepository).findAllByUser_UsersId(userId, pageable);

        log.info("테스트 종료: 유저 펫 목록 조회 성공");
    }
}