package com.dodo.backend.user.entity;

/**
 * 사용자 계정의 현재 상태(Lifecycle)를 정의하는 Enum 클래스입니다.
 * <p>
 * 로그인 허용 여부를 판단하거나, 휴면(Dormant) 및 정지(Suspended) 처리 등
 * 계정 정책에 따른 비즈니스 로직을 수행할 때 기준이 됩니다.
 */
public enum UserStatus {
    ACTIVE, SUSPENDED, DORMANT, DELETED, REGISTER
}