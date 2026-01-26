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
     * 소셜 로그인 인증을 수행하고 결과에 따라 로그인 또는 회원가입 응답을 반환합니다.
     *
     * @param request provider(GOOGLE/NAVER)와 인가 코드(code)를 포함한 요청 객체
     * @return
     * - 기존 회원: 200 OK + Access/Refresh Token <br>
     * - 신규 회원: 202 Accepted + Registration Token (이메일, 이름 포함)
     */
    ResponseEntity<?> socialLogin(AuthRequest.SocialLoginRequest request);

    /**
     * 클라이언트 IP를 기반으로 요청 횟수를 제한합니다.
     */
    void checkRateLimit(String clientIp);
}