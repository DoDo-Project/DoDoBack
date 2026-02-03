package com.dodo.backend.activityhistory.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 활동 기록(ActivityHistory)의 진행 상태를 정의하는 Enum 클래스입니다.
 * <p>
 * 시작 전(BEFORE), 진행 중(IN_PROGRESS), 취소(CANCELED), 완료(COMPLETED) 생명주기 상태를 포함합니다.
 */
@AllArgsConstructor
@Getter
public enum ActivityHistoryStatus {
    BEFORE,
    IN_PROGRESS,
    CANCELED,
    COMPLETED
}