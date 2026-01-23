package com.dodo.backend.auth.service;

import com.dodo.backend.auth.dto.request.AuthRequest.SocialLoginRequest;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialLoginResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialRegisterResponse;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.common.util.JwtTokenProvider;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.user.service.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.auth.exception.AuthErrorCode.INTERNAL_SERVER_ERROR;
import static com.dodo.backend.auth.exception.AuthErrorCode.INVALID_REQUEST;

/**
 * {@link AuthService}의 핵심 구현체로, Naver OAuth와 JWT 기반의 인증을 처리합니다.
 * <p>
 * {@link WebClient}를 사용하여 외부 API와 비동기 통신을 수행하며,
 * 유저의 가입 여부를 판단하여 가입용 토큰 또는 로그인 토큰을 발급합니다.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final WebClient webClient;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${naver.client_id}")
    private String naverClientId;

    @Value("${naver.redirect_uri}")
    private String naverRedirectUri;

    @Value("${naver.client_secret}")
    private String naverClientSecret;

    @Value("${google.client_id}")
    private String googleClientId;

    @Value("${google.redirect_uri}")
    private String googleRedirectUri;

    @Value("${google.client_secret}")
    private String googleClientSecret;

    @Value("${jwt.access-token-validity}")
    private Long accessTokenValidity;

    /**
     * {@inheritDoc}
     * <p>
     * 내부 로직 흐름:
     * 1. 인가 코드 유효성 검증
     * 2. Naver Access Token 획득
     * 3. 유저 프로필 조회 및 가입 여부 확인
     * 4. 신규 유저(202): Registration Token 발급
     * 5. 기존 유저(200): Access/Refresh Token 발급
     */
    @Override
    public ResponseEntity<?> socialLogin(SocialLoginRequest request) {

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new AuthException(INVALID_REQUEST);
        }

        Map<String, Object> userInfo;

        if ("NAVER".equals(request.getProvider())) {
            String naverAccessToken = getNaverAccessToken(request.getCode());
            userInfo = userService.getNaverUserProfile(naverAccessToken);

        } else if ("GOOGLE".equals(request.getProvider())) {
            String googleAccessToken = getGoogleAccessToken(request.getCode());
            userInfo = userService.getGoogleUserProfile(googleAccessToken);

        } else {
            throw new AuthException(INVALID_REQUEST);
        }

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
     * {@inheritDoc}
     * <p>
     * {@code https://nid.naver.com/oauth2.0/token} 끝점으로
     * POST 요청을 보내 Naver 인증 토큰을 수신합니다.
     *
     * @throws AuthException 인증 서버 응답이 유효하지 않거나 통신 장애 발생 시
     */
    @Override
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

    @Override
    public String getGoogleAccessToken(String code) {
        Map response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("oauth2.googleapis.com")
                        .path("/token")
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("client_id", googleClientId)
                        .queryParam("client_secret", googleClientSecret)
                        .queryParam("code", code)
                        .queryParam("redirect_uri", googleRedirectUri)
                        .build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new AuthException(INTERNAL_SERVER_ERROR);
        }
        return (String) response.get("access_token");
    }
}