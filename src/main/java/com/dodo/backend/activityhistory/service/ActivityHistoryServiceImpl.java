package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.ActivityCreateResponse;
import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.activityhistory.exception.ActivityHistoryErrorCode;
import com.dodo.backend.activityhistory.exception.ActivityHistoryException;
import com.dodo.backend.activityhistory.mapper.ActivityHistoryMapper;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.dodo.backend.activityhistory.exception.ActivityHistoryErrorCode.*;

/**
 * {@link ActivityHistoryService} 인터페이스의 구현체 클래스입니다.
 * <p>
 * 반려동물의 활동 기록(ActivityHistory) 생성 및 관리를 담당하는 비즈니스 로직을 수행합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityHistoryServiceImpl implements ActivityHistoryService {

    private final ActivityHistoryRepository activityHistoryRepository;
    private final PetService petService;
    private final UserPetService userPetService;
    private final UserService userService;
    private final ActivityHistoryMapper activityHistoryMapper;

    /**
     * 새로운 활동 기록을 생성합니다.
     * <p>
     * <ol>
     * <li>{@link UserService}를 통해 요청된 User ID로 사용자 정보를 조회합니다. (실패 시 예외 발생)</li>
     * <li>{@link PetService}를 통해 요청된 Pet ID로 반려동물 정보를 조회합니다. (실패 시 예외 발생)</li>
     * <li>{@link UserPetService}를 통해 요청한 유저가 해당 반려동물의 승인된(APPROVED) 주인인지 검증합니다.</li>
     * <li>해당 반려동물이 이미 진행 중인 활동(IN_PROGRESS)이 있는지 확인하여 중복 생성을 방지합니다.</li>
     * <li>검증이 완료되면, 활동 상태를 '시작 전(BEFORE)'으로 설정하여 DB에 저장합니다.</li>
     * </ol>
     *
     * @param userId  요청을 수행하는 사용자의 UUID
     * @param request 생성할 활동 정보가 담긴 요청 DTO (petId, activityType)
     * @return 생성된 활동 기록의 ID와 유형을 포함한 응답 DTO
     * @throws ActivityHistoryException 권한이 없거나({@code CREATE_PERMISSION_DENIED}),
     * 이미 진행 중인 활동이 존재할 경우({@code ALREADY_IN_PROGRESS}) 발생
     */
    @Transactional
    @Override
    public ActivityCreateResponse createActivity(
            UUID userId,
            ActivityCreateRequest request) {

        User user = userService.getUserById(userId);

        Pet pet = petService.getPetById(request.getPetId());

        boolean isOwner = userPetService.isApprovedPetOwner(user.getUsersId(), pet.getPetId());

        if (!isOwner) {
            throw new ActivityHistoryException(CREATE_PERMISSION_DENIED);
        }

        if (activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)) {
            throw new ActivityHistoryException(ALREADY_IN_PROGRESS);
        }

        if (activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)) {
            throw new ActivityHistoryException(ALREADY_EXISTS_BEFORE);
        }

        ActivityHistory activityHistory = request.toEntity(user, pet);

        ActivityHistory savedHistory = activityHistoryRepository.save(activityHistory);

        log.info("활동 기록 생성 완료 - HistoryId: {}, PetId: {}, User: {}",
                savedHistory.getHistoryId(), pet.getPetId(), userId);

        return ActivityCreateResponse.toDto(savedHistory, "활동 기록이 성공적으로 생성되었습니다.");
    }

    /**
     * 활동 기록을 시작 상태(IN_PROGRESS)로 변경합니다.
     * <p>
     * <ol>
     * <li>요청된 History ID로 활동 기록을 조회합니다. (존재하지 않을 경우 {@code HISTORY_NOT_FOUND} 발생)</li>
     * <li>해당 활동 기록의 소유자(User)인지 검증합니다. (권한 없을 경우 {@code START_PERMISSION_DENIED} 발생)</li>
     * <li>활동 상태가 '시작 전(BEFORE)'인지 확인합니다. (이미 진행 중이거나 종료된 경우 {@code ALREADY_IN_PROGRESS} 발생)</li>
     * <li>시작 시간(현재)과 상태(IN_PROGRESS)를 업데이트합니다.</li>
     * </ol>
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 활동 기록 ID
     * @throws ActivityHistoryException 권한이 없거나, 상태가 올바르지 않은 경우 발생
     */
    @Transactional
    @Override
    public void startActivity(UUID userId, Long historyId, ActivityStartRequest request) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!activityHistory.getUser().getUsersId().equals(userId)) {
            throw new ActivityHistoryException(START_PERMISSION_DENIED);
        }

        if (activityHistory.getActivityHistoryStatus() != ActivityHistoryStatus.BEFORE) {
            throw new ActivityHistoryException(ALREADY_IN_PROGRESS);
        }

        activityHistoryMapper.startActivity(
                historyId,
                "IN_PROGRESS",
                request.getStartLatitude(),
                request.getStartLongitude()
        );

        log.info("활동 시작 처리 완료 - HistoryId: {}, User: {}, Lat: {}, Lon: {}",
                historyId, userId, request.getStartLatitude(), request.getStartLongitude());
    }
}