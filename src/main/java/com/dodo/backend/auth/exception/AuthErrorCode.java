package com.dodo.backend.auth.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 인증(Authentication) 및 인가 관련 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 * <p>
 * HTTP 상태 코드와 클라이언트에게 전달할 메시지를 {@code Key-Value} 형태로 관리합니다.
 */
@AllArgsConstructor
@Getter
public enum AuthErrorCode implements BaseErrorCode {


    /**
     * 클라이언트의 요청 형식이 잘못되었거나 파라미터가 유효하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 서버 내부에서 예상치 못한 오류가 발생했을 때 사용합니다.
     * <p>
     * HTTP {@code 500 Internal Server Error}를 반환합니다.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    /**
     * 로그인 시 아이디 또는 비밀번호가 일치하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 401 Unauthorized}를 반환합니다.
     */
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),

    /**
     * 인증 정보(세션, 인증 객체 등)가 없거나 유효하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 401 Unauthorized}를 반환합니다.
     */
    INVALID_AUTH(HttpStatus.UNAUTHORIZED, "인증 정보가 유효하지 않습니다."),

    /**
     * 계정이 정지되었거나 휴면 상태일 때 접근을 차단하며 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    ACCOUNT_RESTRICTED(HttpStatus.FORBIDDEN, "정지된 계정입니다. 또는 휴면 계정입니다."),

    /**
     * 요청한 식별자(ID)에 해당하는 회원을 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 아이디를 찾을 수 없습니다."),

    /**
     * 단시간에 과도한 요청이 발생했을 때 요청을 제한하며 사용합니다.
     * <p>
     * HTTP {@code 429 Too Many Requests}를 반환합니다.
     */
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청 횟수 제한을 초과했습니다. 잠시 후 다시 시도해주세요."),



    /**
     * 리프레시 토큰(Refresh Token)의 유효 기간이 만료되었을 때 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    EXPIRED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "refresh토큰이 만료되었습니다."),

    /**
     * 토큰의 서명이 잘못되었거나 형식이 일치하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),

    /**
     * 토큰 로직 처리 중 리소스를 찾을 수 없는 특정 상황에 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    TOKEN_NOT_FOUND_HANDLE(HttpStatus.NOT_FOUND, "잘못된 요청입니다."),

    /**
     * 요청에 필요한 필수 쿠키가 존재하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    COOKIE_NOT_FOUND(HttpStatus.CONFLICT, "쿠키가 존재하지 않습니다."),

    /**
     * 요청 헤더나 파라미터에 필요한 토큰이 존재하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    TOKEN_NOT_FOUND(HttpStatus.CONFLICT, "토큰이 존재하지 않습니다."),



    /**
     * 등록된 디바이스 정보를 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 디바이스를 찾을 수 없습니다.");


    /**
     * 에러 상황에 해당하는 HTTP 상태 코드입니다.
     */
    private final HttpStatus httpStatus;

    /**
     * 클라이언트에게 전달할 상세 에러 메시지입니다.
     */
    private final String message;
}