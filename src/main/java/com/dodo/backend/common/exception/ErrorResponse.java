package com.dodo.backend.common.exception;

import com.dodo.backend.auth.exception.AuthErrorCode;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

/**
 * API 예외 발생 시 클라이언트에게 전달되는 공통 응답 포맷(DTO)입니다.
 * <p>
 * {@code GlobalExceptionHandler}에서 변환되어 최종적으로
 * {@code {"status": 400, "message": "..."}} 형태의 JSON으로 응답됩니다.
 */
@Getter
@Builder
public class ErrorResponse {

    private final int status;
    private final String message;

    /**
     * 에러 코드(Enum)를 기반으로 {@link ResponseEntity}를 생성하는 팩토리 메서드입니다.
     */
    public static ResponseEntity<ErrorResponse> toResponseEntity(AuthErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getHttpStatus().value())
                        .message(errorCode.getMessage())
                        .build()
                );
    }
}