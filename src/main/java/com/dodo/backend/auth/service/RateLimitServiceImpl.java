package com.dodo.backend.auth.service;

import com.dodo.backend.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.dodo.backend.auth.exception.AuthErrorCode.TOO_MANY_REQUESTS;

/**
 * {@link RateLimitService}의 Redis 기반 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ATTEMPT_KEY_PREFIX = "rate_limit:attempts:";
    private static final String BAN_KEY_PREFIX = "rate_limit:ban:";

    private static final int MAX_ATTEMPTS = 5;
    private static final int BAN_TIME = 10;
    private static final int WINDOW_TIME = 1;

    /**
     * {@inheritDoc}
     * <p>
     * 1. 해당 IP의 차단 여부를 먼저 확인합니다.
     * 2. 시도 횟수를 증가시키고, 5회 이상일 경우 10분간 차단 키를 생성합니다.
     */
    @Override
    public void checkRateLimit(String clientIp) {
        String attemptKey = ATTEMPT_KEY_PREFIX + clientIp;
        String banKey = BAN_KEY_PREFIX + clientIp;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey))) {
            log.warn("차단된 IP의 접근 시도 차단 - IP: {}", clientIp);
            throw new AuthException(TOO_MANY_REQUESTS);
        }

        Long count = redisTemplate.opsForValue().increment(attemptKey);

        if (count != null && count == 1) {
            redisTemplate.expire(attemptKey, WINDOW_TIME, TimeUnit.MINUTES);
        }

        if (count != null && count >= MAX_ATTEMPTS) {
            log.warn("IP 차단 실행 (5회 초과) - IP: {}, 기간: {}분", clientIp, BAN_TIME);
            redisTemplate.opsForValue().set(banKey, "BANNED", BAN_TIME, TimeUnit.MINUTES);
            redisTemplate.delete(attemptKey);
            throw new AuthException(TOO_MANY_REQUESTS);
        }
    }
}