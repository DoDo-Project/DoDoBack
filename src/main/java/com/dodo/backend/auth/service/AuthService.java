package com.dodo.backend.auth.service;

import com.dodo.backend.auth.dto.request.AuthRequest;
import org.springframework.http.ResponseEntity;

/**
 * 소셜 로그인 및 인증 토큰 발급을 담당하는 서비스 인터페이스입니다.
 * <p>
 * 다양한 소셜 인증 제공자로부터 전달받은 인가 정보를 처리하고,
 * 시스템 내부 권한 체계에 맞는 토큰을 생성하여 반환하는 기능을 정의합니다.
 */
public interface AuthService {

    /**
     * 네이버 인가 코드를 받아 소셜 로그인 로직을 수행합니다.
     * <p>
     * 신규 회원이면 추가 정보를 위한 {@code RegistrationToken}을,
     * 기존 회원이면 서비스 이용을 위한 {@code AccessToken}을 발급합니다.
     */
    public ResponseEntity<?> socialLogin(AuthRequest.SocialLoginRequest request);

    /**
     * 네이버 인증 서버와 통신하여 인가 코드를 액세스 토큰으로 교환합니다.
     */
    public String getNaverAccessToken(String code);
}
