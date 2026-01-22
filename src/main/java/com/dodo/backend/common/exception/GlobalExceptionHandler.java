package com.dodo.backend.common.exception;

import com.dodo.backend.auth.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.dodo.backend.auth.exception.AuthErrorCode.INTERNAL_SERVER_ERROR;
import static com.dodo.backend.common.exception.ErrorResponse.toResponseEntity;

/**
 * 전역(Global)에서 발생하는 예외를 중앙에서 감지하고 처리하는 핸들러 클래스입니다.
 * <p>
 * 비즈니스 로직에서 발생한 {@link AuthException} 및 기타 예상치 못한 예외를 포착하여,
 * 클라이언트에게 통일된 JSON 포맷({@link ErrorResponse})으로 변환해 응답합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직에서 의도적으로 발생시킨 {@link AuthException}을 처리합니다.
     * <p>
     * 발생한 예외 내의 에러 코드(status, message)를 사용하여 응답을 생성합니다.
     */
    @ExceptionHandler(AuthException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(AuthException e) {
        log.error("CustomException : {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 사전에 정의되지 않은 예상치 못한 모든 예외(Exception)를 처리합니다.
     * <p>
     * 서버 내부 오류(500)로 간주하여 {@code INTERNAL_SERVER_ERROR}를 반환합니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Internal Server Error : {}", e.getMessage(), e);
        return toResponseEntity(INTERNAL_SERVER_ERROR);
    }
}