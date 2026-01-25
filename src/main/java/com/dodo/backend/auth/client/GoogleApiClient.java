package com.dodo.backend.auth.client;

import com.dodo.backend.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

import static com.dodo.backend.auth.exception.AuthErrorCode.INTERNAL_SERVER_ERROR;

/**
 * {@link SocialApiClient}의 구현체로, Google 소셜 로그인을 담당합니다.
 * <p>
 * 주요 기능:
 * 1. Google 인증 서버와 통신하여 Access Token 발급
 * 2. Google OAuth2 v3 API를 호출하여 사용자 프로필 조회 및 데이터 정규화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleApiClient implements SocialApiClient {

    private final WebClient webClient;

    @Value("${google.client_id}")
    private String clientId;

    @Value("${google.client_secret}")
    private String clientSecret;

    @Value("${google.redirect_uri}")
    private String redirectUri;

    /**
     * {@inheritDoc}
     * <p>
     * "GOOGLE" 공급자에 대한 요청인지 확인합니다.
     */
    @Override
    public boolean support(String provider) {
        return "GOOGLE".equalsIgnoreCase(provider);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. `oauth2.googleapis.com`의 토큰 발급 엔드포인트로 POST 요청
     * 2. Content-Type을 `application/x-www-form-urlencoded`로 설정
     * 3. 응답 JSON에서 `access_token` 추출
     *
     * @throws AuthException 토큰 발급 실패 또는 응답값 누락 시
     */
    @Override
    public String getAccessToken(String code) {
        log.info("구글 액세스 토큰 요청");
        Map response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("oauth2.googleapis.com")
                        .path("/token")
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("code", code)
                        .queryParam("redirect_uri", redirectUri)
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

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. `googleapis.com/oauth2/v3`의 사용자 정보 조회 API 호출
     * 2. Authorization Header에 Bearer Token 포함
     * 3. 구글 특유의 필드명(`picture`)을 공통 규격(`profileUrl`)으로 매핑하여 반환
     */
    @Override
    public Map<String, Object> getUserProfile(String accessToken) {
        log.info("구글 유저 프로필 요청");
        Map response = webClient.get()
                .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> result = new HashMap<>();
        result.put("email", response.get("email"));
        result.put("name", response.get("name"));
        result.put("profileUrl", response.get("picture"));
        return result;
    }
}