package com.dodo.backend.common.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

/**
 * API 예외 발생 시 클라이언트에게 전달되는 공통 응답 포맷(DTO)입니다.
 * <p>
 * 예외 발생 시 {@code GlobalExceptionHandler}를 통해
 * {@code {"status": 400, "message": "..."}} 형태의 통일된 JSON 포맷으로 변환되어 응답됩니다.
 */
@Getter
@Builder
public class ErrorResponse {

    private final int status;
    private final String message;

    /**
     * {@link BaseErrorCode}에 정의된 상태 코드와 메시지를 그대로 사용하여 응답 엔티티를 생성합니다.
     * <p>
     * 주로 비즈니스 로직에서 명시적으로 발생시킨 예외 처리에 사용됩니다.
     *
     * @param errorCode 발생한 예외의 에러 코드 Enum
     * @return HTTP 상태 코드와 에러 메시지가 포함된 ResponseEntity
     */
    public static ResponseEntity<ErrorResponse> toResponseEntity(BaseErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getHttpStatus().value())
                        .message(errorCode.getMessage())
                        .build()
                );
    }

    /**
     * {@link BaseErrorCode}의 상태 코드를 사용하되, 메시지는 별도로 지정하여 응답 엔티티를 생성합니다.
     * <p>
     * 주로 {@code @Valid} 유효성 검증 실패와 같이, 에러 코드는 고정적이나(예: 400)
     * 상세 사유가 동적으로 변하는 경우에 사용됩니다.
     *
     * @param errorCode     사용할 HTTP 상태 코드를 포함한 에러 코드 Enum
     * @param customMessage 응답 바디에 포함될 구체적인 에러 메시지
     * @return HTTP 상태 코드와 커스텀 메시지가 포함된 ResponseEntity
     */
    public static ResponseEntity<ErrorResponse> toResponseEntity(BaseErrorCode errorCode, String customMessage) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getHttpStatus().value())
                        .message(customMessage)
                        .build()
                );
    }
}