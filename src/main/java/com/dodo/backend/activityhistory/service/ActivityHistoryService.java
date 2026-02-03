package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.ActivityCreateResponse;
import com.dodo.backend.user.entity.User;

import java.util.UUID;

/**
 * 활동 기록(ActivityHistory) 관련 비즈니스 로직을 처리하는 서비스 인터페이스입니다.
 */
public interface ActivityHistoryService {

    /**
     * 새로운 활동 기록을 생성합니다.
     * <p>
     * <ol>
     * <li>사용자(User) 및 반려동물(Pet) 존재 여부 검증</li>
     * <li>소유권(UserPet) 검증</li>
     * <li>이미 진행 중인 활동(IN_PROGRESS) 여부 확인</li>
     * <li>활동 기록 엔티티 생성 및 저장 (초기 상태: BEFORE)</li>
     * </ol>
     *
     * @param userId  요청한 사용자의 UUID
     * @param request 활동 생성 요청 정보 (petId, activityType)
     * @return 생성된 활동 기록의 응답 DTO (HistoryId 포함)
     */
    ActivityCreateResponse createActivity(UUID userId, ActivityCreateRequest request);
}