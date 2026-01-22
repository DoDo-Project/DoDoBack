package com.dodo.backend.user.service;

import java.util.Map;

/**
 * 사용자 정보 관리 및 프로필 조회를 위한 서비스 인터페이스입니다.
 * <p>
 * 시스템 내의 사용자 계정 상태를 관리하고,
 * 외부 인증 제공자(OAuth)로부터 유저 정보를 획득하는 기능을 정의합니다.
 */
public interface UserService {

    /**
     * 외부 인증 토큰을 이용해 네이버 유저 프로필을 조회하고 서비스 회원 상태를 반환합니다.
     * <p>
     * 조회된 정보를 바탕으로 기존 회원 여부와 계정의 유효성을 판단합니다.
     *
     * @param accessToken 네이버에서 발급받은 유저 액세스 토큰
     * @return 유저 정보와 가입 상태 정보(isNewMember 등)를 포함한 Map 객체
     */
    public Map<String, Object> getNaverUserProfile(String accessToken);
}
