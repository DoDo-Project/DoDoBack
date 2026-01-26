package com.dodo.backend.auth.service;

import com.dodo.backend.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.dodo.backend.auth.exception.AuthErrorCode.TOO_MANY_REQUESTS;

/**
 * {@link RateLimitService}의 Redis 기반 구현체로, 요청 제한 및 인증 코드 관리를 수행합니다.
 * <p>
 * Redis의 TTL(Time To Live) 기능을 활용하여 IP 차단, 메일 발송 쿨타임,
 * 인증 코드 유효 시간 등을 메모리 상에서 효율적으로 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ATTEMPT_KEY_PREFIX = "rate_limit:attempts:";
    private static final String BAN_KEY_PREFIX = "rate_limit:ban:";
    private static final String EMAIL_LIMIT_PREFIX = "rate_limit:email:";
    private static final String AUTH_CODE_PREFIX = "auth_code:";
    private static final int EMAIL_COOLDOWN = 1;

    /**
     * {@inheritDoc}
     * <p>
     * Redis 내에 해당 IP의 차단 키(Ban Key) 존재 여부를 확인합니다.
     */
    @Override
    public boolean isIpBanned(String clientIp) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BAN_KEY_PREFIX + clientIp));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Redis의 {@code increment} 연산을 통해 시도 횟수를 증가시키며,
     * 최초 시도 시 설정된 시간 동안 키가 유지되도록 만료 시간을 설정합니다.
     */
    @Override
    public Long incrementAttempt(String clientIp, int windowTime) {
        String key = ATTEMPT_KEY_PREFIX + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, windowTime, TimeUnit.MINUTES);
        }
        return count;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 특정 IP를 키로 하여 "BANNED" 상태 값을 Redis에 저장하고, 지정된 기간 동안 유지합니다.
     */
    @Override
    public void banIp(String clientIp, int banTime) {
        redisTemplate.opsForValue().set(BAN_KEY_PREFIX + clientIp, "BANNED", banTime, TimeUnit.MINUTES);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 특정 IP에 대해 누적된 요청 시도 기록 키를 삭제하여 초기화합니다.
     */
    @Override
    public void deleteAttempts(String clientIp) {
        redisTemplate.delete(ATTEMPT_KEY_PREFIX + clientIp);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Redis를 통해 해당 이메일의 메일 발송 제한 키 존재 여부를 확인합니다.
     */
    @Override
    public boolean isEmailCooldownActive(String email) {
        String key = EMAIL_LIMIT_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * {@inheritDoc}
     * <p>
     * 이메일 발송 성공 시 Redis에 1분간 유효한 제한 키를 설정하여 재발송을 방지합니다.
     */
    @Override
    public void setEmailCooldown(String email) {
        String key = EMAIL_LIMIT_PREFIX + email;
        redisTemplate.opsForValue().set(key, "SENT", EMAIL_COOLDOWN, TimeUnit.MINUTES);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 생성된 인증 번호를 Redis에 저장하며, 지정된 시간(duration)이 지나면 자동으로 소멸되도록 관리합니다.
     */
    @Override
    public void saveVerificationCode(String email, String code, int duration) {
        String key = AUTH_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, duration, TimeUnit.MINUTES);
    }
}