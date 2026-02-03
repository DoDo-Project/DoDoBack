package com.dodo.backend.activityhistory.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 반려동물의 활동 종류를 정의하는 Enum 클래스입니다.
 * <p>
 * 산책(WALKING), 수면(SLEEPING) 등의 활동 유형을 포함합니다.
 */
@AllArgsConstructor
@Getter
public enum ActivityType {
    WALKING, SLEEPING
}