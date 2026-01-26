package com.dodo.backend.auth.service;

import com.dodo.backend.auth.exception.AuthException;
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

import static com.dodo.backend.auth.exception.AuthErrorCode.TOO_MANY_REQUESTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * {@link RateLimitServiceImpl}의 비즈니스 로직을 검증하기 위한 단위 테스트 클래스입니다.
 * <p>
 * Mockito를 사용하여 Redis 인프라 계층을 모킹(Mocking)하고,
 * IP 기반의 횟수 제한 및 차단 정책이 올바르게 작동하는지 확인합니다.
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
    private final String attemptKey = "rate_limit:attempts:" + clientIp;
    private final String banKey = "rate_limit:ban:" + clientIp;

    /**
     * 각 테스트 실행 전 RedisTemplate의 동작을 정의합니다.
     */
    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /**
     * 정상적인 요청 흐름을 테스트합니다.
     * <p>
     * 1회 시도 시 Redis에 횟수가 기록되고, 최초 시도이므로 만료 시간이 설정되어야 합니다.
     */
    @Test
    @DisplayName("정상 요청 테스트: 시도 횟수가 임계치 미만이면 통과한다.")
    void checkRateLimit_Success() {
        log.info("시나리오: 정상 IP({})의 최초 요청 발생", clientIp);

        // given
        given(redisTemplate.hasKey(banKey)).willReturn(false);
        given(valueOperations.increment(attemptKey)).willReturn(1L);

        // when
        rateLimitService.checkRateLimit(clientIp);

        // then
        log.info("검증: 시도 횟수 증가 및 만료 시간 설정 확인");
        verify(redisTemplate).expire(eq(attemptKey), anyLong(), eq(TimeUnit.MINUTES));
    }

    /**
     * 이미 차단된 IP의 요청을 테스트합니다.
     * <p>
     * Redis에 Ban 키가 존재할 경우, 이후 로직을 수행하지 않고 즉시 예외를 던져야 합니다.
     */
    @Test
    @DisplayName("차단 확인 테스트: 이미 차단된 IP는 즉시 예외를 발생시킨다.")
    void checkRateLimit_AlreadyBanned() {
        log.info("시나리오: 이미 차단된 IP({})의 재접속 시도", clientIp);

        // given
        given(redisTemplate.hasKey(banKey)).willReturn(true);

        // when & then
        AuthException exception = assertThrows(AuthException.class, () ->
                rateLimitService.checkRateLimit(clientIp)
        );

        log.info("결과: 에러 메시지 확인 -> {}", exception.getErrorCode().getMessage());
        assertEquals(TOO_MANY_REQUESTS, exception.getErrorCode());
        verify(valueOperations, never()).increment(anyString());
    }

    /**
     * 횟수 초과 시 자동 차단 로직을 테스트합니다.
     * <p>
     * 5회째 요청이 들어오면 Redis에 10분짜리 Ban 키를 생성하고 기존 시도 기록을 삭제해야 합니다.
     */
    @Test
    @DisplayName("차단 실행 테스트: 5회 이상 시도 시 IP를 10분간 차단한다.")
    void checkRateLimit_TriggerBan() {
        log.info("시나리오: IP({})의 시도 횟수 5회 도달 (임계치 초과)", clientIp);

        // given
        given(redisTemplate.hasKey(banKey)).willReturn(false);
        given(valueOperations.increment(attemptKey)).willReturn(5L);

        // when & then
        AuthException exception = assertThrows(AuthException.class, () ->
                rateLimitService.checkRateLimit(clientIp)
        );

        log.info("결과: TOO_MANY_REQUESTS 예외 발생 및 10분 차단 설정 확인");
        assertEquals(TOO_MANY_REQUESTS, exception.getErrorCode());

        // then: Ban 키 생성 및 시도 횟수 키 삭제 검증
        verify(valueOperations).set(eq(banKey), eq("BANNED"), eq(10L), eq(TimeUnit.MINUTES));
        verify(redisTemplate).delete(attemptKey);
    }
}