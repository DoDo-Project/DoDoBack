package com.dodo.backend.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증(Authentication) 및 인가(Authorization) 로직 수행 중 발생하는 전용 예외 클래스입니다.
 * <p>
 * 비즈니스 로직에서 예외 상황(로그인 실패, 토큰 만료 등) 발생 시 이 클래스를 throw 합니다.
 * {@code GlobalExceptionHandler}에서 이 예외를 잡아서 표준화된 JSON 응답으로 변환합니다.
 */
@AllArgsConstructor
@Getter
public class AuthException extends RuntimeException {

    /**
     * 발생한 예외의 구체적인 종류(상태 코드, 메시지)를 담고 있는 Enum입니다.
     */
    private final AuthErrorCode errorCode;
}