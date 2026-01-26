package com.dodo.backend.auth.service;

import com.dodo.backend.auth.dto.request.AuthRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.LogoutRequest;
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
     * 클라이언트 IP를 기반으로 요청 횟수를 검증하여 비정상적인 접근을 제한합니다.
     * <p>
     * 단시간 내에 발생하는 과도한 요청을 감지하고, 시스템 보안 정책에 따라
     * 일정 시간 동안 해당 IP의 서비스 이용을 차단합니다.
     *
     * @param clientIp 요청을 보낸 클라이언트의 IP 주소
     * @throws com.dodo.backend.auth.exception.AuthException 요청 횟수가 임계치를 초과했거나 차단된 IP일 경우 발생
     */
    void checkRateLimit(String clientIp);

    /**
     * 로그아웃을 수행합니다.
     * Refresh Token 삭제 및 Access Token 블랙리스트 처리를 수행합니다.
     *
     * @param request 로그아웃할 리프레시 토큰 DTO
     * @param accessToken 현재 사용 중인 액세스 토큰 (Header에서 추출)
     */
    void logout(LogoutRequest request, String accessToken);
}