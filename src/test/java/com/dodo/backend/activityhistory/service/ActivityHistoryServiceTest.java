package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse;
import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.activityhistory.entity.ActivityType;
import com.dodo.backend.activityhistory.exception.ActivityHistoryErrorCode;
import com.dodo.backend.activityhistory.exception.ActivityHistoryException;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link ActivityHistoryService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class ActivityHistoryServiceTest {

    @InjectMocks
    private ActivityHistoryServiceImpl activityHistoryService;

    @Mock
    private ActivityHistoryRepository activityHistoryRepository;

    @Mock
    private PetService petService;

    @Mock
    private UserPetService userPetService;

    @Mock
    private UserService userService;

    /**
     * 활동 기록 생성 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 성공: 정상적인 요청 시 상태가 BEFORE인 기록이 생성된다.")
    void createActivity_Success() {
        log.info("활동 기록 생성 성공 케이스 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType(ActivityType.WALKING)
                .build();

        ActivityHistory savedHistory = request.toEntity(user, pet);
        ReflectionTestUtils.setField(savedHistory, "historyId", 100L);

        log.info("유저와 펫이 존재하고, 권한이 있으며, 중복된 활동이 없는 상황을 설정합니다.");
        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(false);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)).willReturn(false);
        given(activityHistoryRepository.save(any(ActivityHistory.class))).willReturn(savedHistory);

        // when
        log.info("활동 기록 생성 서비스 로직을 호출합니다.");
        ActivityHistoryResponse.ActivityCreateResponse response = activityHistoryService.createActivity(userId, request);

        // then
        log.info("생성된 History ID가 반환되었는지 확인하고, 저장 로직이 호출되었는지 검증합니다.");
        assertNotNull(response);
        assertEquals(100L, response.getHistoryId());
        assertEquals(ActivityType.WALKING, response.getActivityType());

        verify(userService, times(1)).getUserById(userId);
        verify(petService, times(1)).getPetById(petId);
        verify(userPetService, times(1)).isApprovedPetOwner(userId, petId);
        verify(activityHistoryRepository, times(1)).save(any(ActivityHistory.class));
        log.info("활동 기록 생성 성공 테스트가 통과되었습니다.");
    }

    /**
     * 권한이 없는 사용자가 활동 기록 생성을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 실패: 펫의 소유자가 아닌 경우 권한 예외가 발생한다.")
    void createActivity_Fail_PermissionDenied() {
        log.info("권한 없음으로 인한 활동 생성 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType(ActivityType.WALKING)
                .build();

        log.info("유저와 펫은 존재하지만, 소유자가 아니라고 설정합니다.");
        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        log.info("생성 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );

        // then
        log.info("발생한 예외 코드가 CREATE_PERMISSION_DENIED인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.CREATE_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).save(any(ActivityHistory.class));
        log.info("권한 없음 실패 테스트가 통과되었습니다.");
    }

    /**
     * 이미 진행 중(IN_PROGRESS)인 활동이 있을 때 생성 시도를 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 실패: 이미 진행 중인 활동이 있는 경우 예외가 발생한다.")
    void createActivity_Fail_AlreadyInProgress() {
        log.info("중복 활동(진행 중)으로 인한 생성 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType(ActivityType.WALKING)
                .build();

        log.info("소유자 권한은 있으나, 이미 진행 중인 활동이 존재한다고 설정합니다.");
        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(true);

        // when
        log.info("생성 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );

        // then
        log.info("발생한 예외 코드가 ALREADY_IN_PROGRESS인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.ALREADY_IN_PROGRESS, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).save(any(ActivityHistory.class));
        log.info("진행 중인 활동 중복 실패 테스트가 통과되었습니다.");
    }

    /**
     * 이미 시작 대기 중(BEFORE)인 활동이 있을 때 생성 시도를 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 실패: 이미 시작 대기 중인 활동이 있는 경우 예외가 발생한다.")
    void createActivity_Fail_AlreadyExistsBefore() {
        log.info("중복 활동(시작 대기 중)으로 인한 생성 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType(ActivityType.WALKING)
                .build();

        log.info("진행 중인 활동은 없으나, 이미 시작 대기 중(BEFORE)인 활동이 존재한다고 설정합니다.");
        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(false);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)).willReturn(true);

        // when
        log.info("생성 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );

        // then
        log.info("발생한 예외 코드가 ALREADY_EXISTS_BEFORE인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.ALREADY_EXISTS_BEFORE, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).save(any(ActivityHistory.class));
        log.info("시작 대기 중 활동 중복 실패 테스트가 통과되었습니다.");
    }
}