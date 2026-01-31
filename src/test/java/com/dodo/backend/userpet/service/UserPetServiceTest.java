package com.dodo.backend.userpet.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import com.dodo.backend.userpet.exception.UserPetErrorCode;
import com.dodo.backend.userpet.exception.UserPetException;
import com.dodo.backend.userpet.mapper.UserPetMapper;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link UserPetService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 * <p>
 * 유저와 펫 사이의 멤버십 관계(UserPet) 생성, 가족 초대 코드 발급,
 * 목록 조회 및 승인/거절 프로세스를 단위 테스트로 검증합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class UserPetServiceTest {

    @InjectMocks
    private UserPetServiceImpl userPetService;

    @Mock
    private UserPetRepository userPetRepository;

    @Mock
    private UserPetMapper userPetMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    /**
     * UserPet 관계 등록 성공 시나리오를 테스트합니다.
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

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userPetService.registerUserPet(userId, pet, status);

        // then
        ArgumentCaptor<UserPet> captor = ArgumentCaptor.forClass(UserPet.class);
        verify(userPetRepository).save(captor.capture());

        UserPet savedUserPet = captor.getValue();
        assertEquals(user, savedUserPet.getUser());
        assertEquals(pet, savedUserPet.getPet());
        assertEquals(status, savedUserPet.getRegistrationStatus());
        assertNotNull(savedUserPet.getId());

        log.info("테스트 종료: UserPet 등록 성공 시나리오");
    }

    /**
     * 가족 초대 코드 발급 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 성공: 승인된 멤버라면 코드가 생성되고 Redis에 저장된다.")
    void generateInvitationCode_Success() {
        log.info("테스트 시작: 초대 코드 발급 성공 시나리오");

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
        assertNotNull(result.get("code"));
        assertNotNull(result.get("expiresIn"));

        verify(valueOperations, times(2)).set(anyString(), anyString(), any(Duration.class));

        log.info("테스트 종료: 초대 코드 발급 성공 시나리오");
    }

    /**
     * 가족 구성원이 아닌 유저가 초대를 시도할 때 예외 발생을 테스트합니다.
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
     */
    @Test
    @DisplayName("초대 코드 발급 실패: 이미 유효한 코드가 존재하면 중복 예외가 발생한다.")
    void generateInvitationCode_Fail_AlreadyExists() {
        log.info("테스트 시작: 초대 코드 발급 실패 (코드 중복)");

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

        log.info("테스트 종료: 초대 코드 발급 실패 (코드 중복)");
    }

    /**
     * 초대 코드 입력 및 가족 신청(대기) 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("초대 수락 성공: 유효한 코드 입력 시 대기(PENDING) 상태로 등록하고 펫 ID를 반환한다.")
    void registerByInvitation_Success() {
        log.info("테스트 시작: 초대 수락 및 등록 성공 시나리오");

        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        String petIdStr = "100";
        Long petId = 100L;

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "usersId", userId);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(petIdStr);

        given(userPetRepository.existsById(any(UserPetId.class))).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        Long resultPetId = userPetService.registerByInvitation(userId, invitationCode);

        // then
        assertEquals(petId, resultPetId);

        ArgumentCaptor<UserPet> captor = ArgumentCaptor.forClass(UserPet.class);
        verify(userPetRepository).save(captor.capture());

        assertEquals(RegistrationStatus.PENDING, captor.getValue().getRegistrationStatus());
        assertEquals(petId, captor.getValue().getPet().getPetId());

        log.info("테스트 종료: 초대 수락 및 등록 성공 시나리오");
    }

    /**
     * 만료되거나 존재하지 않는 초대 코드를 입력했을 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("초대 수락 실패: 코드가 존재하지 않거나 만료된 경우 예외가 발생한다.")
    void registerByInvitation_Fail_InvalidCode() {
        log.info("테스트 시작: 초대 수락 실패 (코드 없음/만료)");

        // given
        UUID userId = UUID.randomUUID();
        String invalidCode = "INVALID";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);

        // when & then
        UserPetException exception = assertThrows(UserPetException.class, () ->
                userPetService.registerByInvitation(userId, invalidCode)
        );

        assertEquals(UserPetErrorCode.INVITATION_NOT_FOUND, exception.getErrorCode());

        log.info("테스트 종료: 초대 수락 실패 (코드 없음/만료)");
    }

    /**
     * 이미 가족으로 등록된 사용자가 초대를 수락하려 할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("초대 수락 실패: 이미 가족 멤버인 경우 예외가 발생한다.")
    void registerByInvitation_Fail_AlreadyMember() {
        log.info("테스트 시작: 초대 수락 실패 (이미 가족 멤버)");

        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        String petIdStr = "100";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(petIdStr);

        given(userPetRepository.existsById(any(UserPetId.class))).willReturn(true);

        // when & then
        UserPetException exception = assertThrows(UserPetException.class, () ->
                userPetService.registerByInvitation(userId, invitationCode)
        );

        assertEquals(UserPetErrorCode.ALREADY_FAMILY_MEMBER, exception.getErrorCode());
        verify(userPetRepository, times(0)).save(any(UserPet.class));

        log.info("테스트 종료: 초대 수락 실패 (이미 가족 멤버)");
    }

    /**
     * 유저의 펫 목록 조회 시나리오를 테스트합니다. (수정됨: APPROVED 상태 필터링)
     */
    @Test
    @DisplayName("유저 펫 목록 조회 성공: 승인된(APPROVED) 펫 목록만 페이징되어 반환된다.")
    void getUserPets_Success() {
        log.info("테스트 시작: 유저 펫 목록 조회 성공 시나리오");

        // given
        UUID userId = UUID.randomUUID();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        java.util.List<UserPet> emptyList = java.util.Collections.emptyList();
        org.springframework.data.domain.Page<UserPet> mockPage =
                new org.springframework.data.domain.PageImpl<>(emptyList, pageable, 0);

        given(userPetRepository.findAllByUser_UsersIdAndRegistrationStatus(
                userId,
                RegistrationStatus.APPROVED,
                pageable
        )).willReturn(mockPage);

        // when
        Map<String, Object> result = userPetService.getUserPets(userId, pageable);

        // then
        assertNotNull(result);
        assertTrue(result.containsKey("userPetPage"));
        assertEquals(mockPage, result.get("userPetPage"));

        verify(userPetRepository).findAllByUser_UsersIdAndRegistrationStatus(
                userId,
                RegistrationStatus.APPROVED,
                pageable
        );

        log.info("테스트 종료: 유저 펫 목록 조회 성공 시나리오");
    }

    /**
     * 가족 승인 요청 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("가족 요청 처리 성공: 승인(APPROVED) 요청 시 Mapper를 통해 상태가 업데이트된다.")
    void approveFamilyMember_Success() {
        log.info("테스트 시작: 가족 요청 처리 성공 (승인)");

        // given
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Long petId = 100L;
        String action = "APPROVED";

        UserPet requester = UserPet.builder().registrationStatus(RegistrationStatus.APPROVED).build();
        UserPet target = UserPet.builder().registrationStatus(RegistrationStatus.PENDING).build();

        given(userPetRepository.findById(new UserPetId(requesterId, petId))).willReturn(Optional.of(requester));
        given(userPetRepository.findById(new UserPetId(targetUserId, petId))).willReturn(Optional.of(target));

        // when
        String result = userPetService.approveOrRejectFamilyMember(requesterId, petId, targetUserId, action);

        // then
        assertEquals("가족 신청을 승인했습니다.", result);
        verify(userPetMapper).updateRegistrationStatus(targetUserId, petId, "APPROVED");

        log.info("테스트 종료: 가족 요청 처리 성공 (승인)");
    }

    /**
     * 가족 거절 요청 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("가족 요청 처리 성공: 거절(REJECTED) 요청 시 Mapper를 통해 상태가 REJECTED로 업데이트된다.")
    void rejectFamilyMember_Success() {
        log.info("테스트 시작: 가족 요청 처리 성공 (거절)");

        // given
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Long petId = 100L;
        String action = "REJECTED";

        UserPet requester = UserPet.builder().registrationStatus(RegistrationStatus.APPROVED).build();
        UserPet target = UserPet.builder().registrationStatus(RegistrationStatus.PENDING).build();

        given(userPetRepository.findById(new UserPetId(requesterId, petId))).willReturn(Optional.of(requester));
        given(userPetRepository.findById(new UserPetId(targetUserId, petId))).willReturn(Optional.of(target));

        // when
        String result = userPetService.approveOrRejectFamilyMember(requesterId, petId, targetUserId, action);

        // then
        assertEquals("가족 신청을 거절했습니다.", result);
        verify(userPetMapper).updateRegistrationStatus(targetUserId, petId, "REJECTED");

        log.info("테스트 종료: 가족 요청 처리 성공 (거절)");
    }

    /**
     * 권한 없는 유저(가족 구성원이 아니거나 PENDING 상태)가 승인을 시도할 때 예외를 테스트합니다.
     */
    @Test
    @DisplayName("가족 요청 처리 실패: 요청자가 승인 권한이 없으면 예외가 발생한다.")
    void approveFamilyMember_Fail_NoPermission() {
        log.info("테스트 시작: 가족 요청 처리 실패 (권한 없음)");

        // given
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Long petId = 100L;
        String action = "APPROVED";

        // PENDING 상태인 유저(권한 없음)
        UserPet requester = UserPet.builder().registrationStatus(RegistrationStatus.PENDING).build();

        given(userPetRepository.findById(new UserPetId(requesterId, petId))).willReturn(Optional.of(requester));

        // when & then
        UserPetException exception = assertThrows(UserPetException.class, () ->
                userPetService.approveOrRejectFamilyMember(requesterId, petId, targetUserId, action)
        );

        assertEquals(UserPetErrorCode.INVITE_PERMISSION_DENIED, exception.getErrorCode());

        log.info("테스트 종료: 가족 요청 처리 실패 (권한 없음)");
    }

    /**
     * 존재하지 않는 대상을 승인/거절하려고 할 때 예외를 테스트합니다.
     */
    @Test
    @DisplayName("가족 요청 처리 실패: 대상 유저가 존재하지 않거나 PENDING 상태가 아니면 예외가 발생한다.")
    void approveFamilyMember_Fail_TargetNotFound() {
        log.info("테스트 시작: 가족 요청 처리 실패 (대상 없음)");

        // given
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Long petId = 100L;
        String action = "APPROVED";

        UserPet requester = UserPet.builder().registrationStatus(RegistrationStatus.APPROVED).build();

        given(userPetRepository.findById(new UserPetId(requesterId, petId))).willReturn(Optional.of(requester));
        given(userPetRepository.findById(new UserPetId(targetUserId, petId))).willReturn(Optional.empty());

        // when & then
        UserPetException exception = assertThrows(UserPetException.class, () ->
                userPetService.approveOrRejectFamilyMember(requesterId, petId, targetUserId, action)
        );

        assertEquals(UserPetErrorCode.INVITEE_NOT_FOUND, exception.getErrorCode());

        log.info("테스트 종료: 가족 요청 처리 실패 (대상 없음)");
    }
}