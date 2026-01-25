package com.dodo.backend.common.exception;

import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.user.exception.UserErrorCode;
import com.dodo.backend.user.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.dodo.backend.auth.exception.AuthErrorCode.INTERNAL_SERVER_ERROR;
import static com.dodo.backend.common.exception.ErrorResponse.toResponseEntity;

/**
 * 전역(Global)에서 발생하는 예외를 중앙에서 감지하고 처리하는 핸들러 클래스입니다.
 * <p>
 * 비즈니스 로직 예외({@link AuthException}, {@link UserException}) 및
 * 입력값 유효성 검증 예외를 포착하여 통일된 JSON 포맷으로 변환해 응답합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 인증 및 인가 로직에서 발생하는 {@link AuthException}을 처리합니다.
     */
    @ExceptionHandler(AuthException.class)
    protected ResponseEntity<ErrorResponse> handleAuthException(AuthException e) {
        log.error("AuthException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 유저 도메인 비즈니스 로직에서 발생하는 {@link UserException}을 처리합니다.
     */
    @ExceptionHandler(UserException.class)
    protected ResponseEntity<ErrorResponse> handleUserException(UserException e) {
        log.error("UserException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * {@code @Valid} 어노테이션을 통한 요청 데이터 유효성 검증 실패 시 발생하는 예외를 처리합니다.
     * <p>
     * DTO에 설정된 구체적인 에러 메시지를 추출하고,
     * {@link UserErrorCode#INVALID_PARAMETER}의 상태 코드를 사용하여 응답을 생성합니다.
     *
     * @param e 유효성 검증 실패 정보를 담은 예외 객체
     * @return 400 Bad Request 상태 코드와 DTO의 에러 메시지를 포함한 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();

        String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "잘못된 요청입니다.";

        log.warn("Validation failed: {}", errorMessage);

        return toResponseEntity(UserErrorCode.INVALID_PARAMETER, errorMessage);
    }

    /**
     * 처리되지 않은 예상치 못한 모든 예외(Exception)를 처리합니다.
     * <p>
     * 500 Internal Server Error로 응답합니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Internal Server Error : {}", e.getMessage(), e);
        return toResponseEntity(INTERNAL_SERVER_ERROR);
    }
}