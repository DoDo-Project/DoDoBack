package com.dodo.backend.auth.service;

import com.dodo.backend.auth.dto.request.AuthRequest.SocialLoginRequest;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialLoginResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialRegisterResponse;
import com.dodo.backend.auth.exception.AuthErrorCode;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.common.util.JwtTokenProvider;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.auth.exception.AuthErrorCode.INTERNAL_SERVER_ERROR;
import static com.dodo.backend.auth.exception.AuthErrorCode.INVALID_REQUEST;

/**
 * 소셜 로그인 및 토큰 발급을 담당하는 핵심 서비스 클래스입니다.
 * <p>
 * 외부 OAuth 서버(Naver)와 통신하여 인증을 수행하고, 유저 상태(신규/기존)에 따라
 * 회원가입 토큰(202) 또는 액세스 토큰(200)을 분기하여 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WebClient webClient;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${naver.client_id}")
    private String naverClientId;

    @Value("${naver.redirect_uri}")
    private String naverRedirectUri;

    @Value("${naver.client_secret}")
    private String naverClientSecret;

    @Value("${jwt.access-token-validity}")
    private Long accessTokenValidity;

    /**
     * 네이버 인가 코드를 받아 소셜 로그인 로직을 수행합니다.
     * <p>
     * 신규 회원이면 추가 정보를 위한 {@code RegistrationToken}을,
     * 기존 회원이면 서비스 이용을 위한 {@code AccessToken}을 발급합니다.
     */
    public ResponseEntity<?> socialLogin(SocialLoginRequest request) {

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new AuthException(INVALID_REQUEST);
        }

        String naverAccessToken = getNaverAccessToken(request.getCode());
        Map<String, Object> userInfo = userService.getNaverUserProfile(naverAccessToken);
        boolean isNewMember = (boolean) userInfo.get("isNewMember");

        if (isNewMember) {
            String email  = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String registrationToken = jwtTokenProvider.createRegisterToken(email);

            return ResponseEntity.status(202).body(
                    SocialRegisterResponse.builder()
                            .message("추가 정보가 필요합니다.")
                            .email(email)
                            .name(name)
                            .registrationToken(registrationToken)
                            .tokenExpiresIn(accessTokenValidity / 2000)
                            .build()
            );
        } else {

            UUID userId = (UUID) userInfo.get("userId");
            String role = (String) userInfo.get("role");
            String profileUrl = (String) userInfo.get("profileUrl");

            String accessToken = jwtTokenProvider.createAccessToken(userId, role);
            String refreshToken = jwtTokenProvider.createRefreshToken(userId);

            return ResponseEntity.ok(
                    SocialLoginResponse.builder()
                            .profileUrl(profileUrl)
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .accessTokenExpiresIn(accessTokenValidity / 1000)
                            .build()
            );
        }
    }

    /**
     * 네이버 인증 서버와 통신하여 인가 코드를 액세스 토큰으로 교환합니다.
     */
    public String getNaverAccessToken(String code) {
        Map response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nid.naver.com")
                        .path("/oauth2.0/token")
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("client_id", naverClientId)
                        .queryParam("client_secret", naverClientSecret)
                        .queryParam("code", code)
                        .queryParam("state", "test_state")
                        .queryParam("redirect_uri", naverRedirectUri)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new AuthException(INTERNAL_SERVER_ERROR);
        }

        return (String) response.get("access_token");
    }
}