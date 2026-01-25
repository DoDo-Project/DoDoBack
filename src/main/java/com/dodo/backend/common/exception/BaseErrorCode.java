package com.dodo.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 모든 도메인 에러 코드 Enum이 구현해야 할 공통 인터페이스입니다.
 * <p>
 * 이 인터페이스를 통해 각기 다른 도메인의 에러 코드들을
 * {@link ErrorResponse}에서 통일된 방식으로 처리할 수 있습니다.
 */
public interface BaseErrorCode {
    /**
     * 에러 상황에 해당하는 HTTP 상태 코드를 반환합니다.
     */
    HttpStatus getHttpStatus();

    /**
     * 클라이언트에게 전달할 상세 에러 메시지를 반환합니다.
     */
    String getMessage();

    /**
     * Enum 상수의 이름을 반환합니다. (Enum 클래스 기본 제공 메서드)
     */
    String name();
}