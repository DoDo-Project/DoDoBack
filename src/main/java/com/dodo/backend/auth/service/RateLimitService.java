package com.dodo.backend.auth.service;

/**
 * 서비스의 보안 및 안정성을 위해 API 요청 횟수를 제한하는 인터페이스입니다.
 */
public interface RateLimitService {

    /**
     * 특정 클라이언트 IP의 요청 횟수를 검증하고, 초과 시 차단 로직을 수행합니다.
     *
     * @param clientIp 요청자의 IP 주소
     */
    void checkRateLimit(String clientIp);
}