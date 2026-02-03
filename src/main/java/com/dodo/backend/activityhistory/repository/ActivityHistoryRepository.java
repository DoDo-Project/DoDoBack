package com.dodo.backend.activityhistory.repository;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * {@link ActivityHistory} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface ActivityHistoryRepository extends JpaRepository<ActivityHistory, Long> {

    /**
     * 특정 반려동물의 활동 상태가 주어진 상태(status)와 일치하는 기록이 존재하는지 확인합니다.
     * <p>
     * 주로 이미 진행 중인 활동(IN_PROGRESS)이 있는지 중복 체크할 때 사용됩니다.
     * </p>
     *
     * @param pet    확인할 반려동물 엔티티
     * @param status 확인할 활동 상태 (예: IN_PROGRESS)
     * @return 해당 상태의 활동 기록이 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByPetAndActivityHistoryStatus(Pet pet, ActivityHistoryStatus status);
}