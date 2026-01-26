package com.dodo.backend.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * {@link RateLimitServiceImpl}의 Redis 데이터 조작 로직을 검증하는 테스트 클래스입니다.
 * <p>
 * Redis 인프라 계층과의 상호작용이 설정된 정책(TTL, Key 생성 등)에 따라
 * 정확하게 수행되는지 확인합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class RateLimitServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RateLimitServiceImpl rateLimitService;

    private final String clientIp = "127.0.0.1";
    private final String banKey = "rate_limit:ban:127.0.0.1";
    private final String attemptKey = "rate_limit:attempts:127.0.0.1";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /**
     * IP 차단 여부 조회 로직을 테스트합니다.
     * <p>
     * Redis에 해당 IP의 Ban 키가 존재할 경우 {@code true}를 반환해야 합니다.
     */
    @Test
    @DisplayName("IP 차단 확인 테스트: Ban 키 존재 시 true를 반환한다.")
    void isIpBanned_True() {
        // given
        given(redisTemplate.hasKey(banKey)).willReturn(true);

        // when
        boolean isBanned = rateLimitService.isIpBanned(clientIp);

        // then
        assertEquals(true, isBanned);
    }

    /**
     * 시도 횟수 증가 및 만료 시간 설정을 테스트합니다.
     * <p>
     * 최초 시도 시 횟수가 1로 기록되고 윈도우 시간(TTL)이 설정되어야 합니다.
     */
    @Test
    @DisplayName("시도 횟수 증가 테스트: 최초 시도 시 TTL이 설정된다.")
    void incrementAttempt_FirstAttempt() {
        // given
        given(valueOperations.increment(attemptKey)).willReturn(1L);

        // when
        Long count = rateLimitService.incrementAttempt(clientIp, 1);

        // then
        assertEquals(1L, count);
        verify(redisTemplate).expire(eq(attemptKey), eq(1L), eq(TimeUnit.MINUTES));
    }

    /**
     * 특정 IP의 차단 등록 로직을 테스트합니다.
     * <p>
     * 지정된 시간(10분) 동안 Redis에 BANNED 상태 값이 저장되어야 합니다.
     */
    @Test
    @DisplayName("IP 차단 등록 테스트: 10분간 Redis에 차단 상태를 저장한다.")
    void banIp_Success() {
        // when
        rateLimitService.banIp(clientIp, 10);

        // then
        verify(valueOperations).set(eq(banKey), eq("BANNED"), eq(10L), eq(TimeUnit.MINUTES));
    }

    /**
     * 이메일 발송 쿨타임 활성화 여부를 테스트합니다.
     * <p>
     * Redis에 이메일 제한 키가 존재하면 {@code true}를 반환해야 합니다.
     */
    @Test
    @DisplayName("이메일 쿨타임 확인 테스트: 제한 키 존재 시 true를 반환한다.")
    void isEmailCooldownActive_True() {
        // given
        String email = "test@dodo.com";
        String emailKey = "rate_limit:email:test@dodo.com";
        given(redisTemplate.hasKey(emailKey)).willReturn(true);

        // when
        boolean isActive = rateLimitService.isEmailCooldownActive(email);

        // then
        assertEquals(true, isActive);
    }
}