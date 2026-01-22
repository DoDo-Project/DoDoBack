package com.dodo.backend.user.entity;

/**
 * 시스템 내에서 사용자의 권한 등급(Role)을 정의하는 Enum 클래스입니다.
 * <p>
 * API 접근 제어(Authorization) 및 비즈니스 로직에서
 * 일반 사용자(USER)와 관리자(ADMIN)를 구분하는 기준으로 사용됩니다.
 */
public enum UserRole {
    USER, ADMIN
}