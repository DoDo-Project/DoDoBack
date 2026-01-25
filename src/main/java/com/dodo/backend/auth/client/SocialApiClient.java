package com.dodo.backend.auth.client;

import java.util.Map;

public interface SocialApiClient {
    /**
     * 이 구현체가 해당 provider(NAVER, GOOGLE 등)를 처리할 수 있는지 확인합니다.
     */
    boolean support(String provider);

    /**
     * 인가 코드로 액세스 토큰을 받아옵니다.
     */
    String getAccessToken(String code);

    /**
     * 액세스 토큰으로 유저 프로필 정보를 받아옵니다.
     */
    Map<String, Object> getUserProfile(String accessToken);
}