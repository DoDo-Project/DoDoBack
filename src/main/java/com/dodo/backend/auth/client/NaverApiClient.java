package com.dodo.backend.auth.client;

import com.dodo.backend.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

import static com.dodo.backend.auth.exception.AuthErrorCode.INTERNAL_SERVER_ERROR;

/**
 * {@link SocialApiClient}의 구현체로, Naver 소셜 로그인을 담당합니다.
 * <p>
 * 주요 기능:
 * 1. Naver 인증 서버와 통신하여 Access Token 발급
 * 2. Naver Open API를 호출하여 사용자 프로필 조회 및 데이터 정규화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverApiClient implements SocialApiClient {

    private final WebClient webClient;

    @Value("${naver.client_id}")
    private String clientId;

    @Value("${naver.client_secret}")
    private String clientSecret;

    @Value("${naver.redirect_uri}")
    private String redirectUri;

    /**
     * {@inheritDoc}
     * <p>
     * "NAVER" 공급자에 대한 요청인지 확인합니다.
     */
    @Override
    public boolean support(String provider) {
        return "NAVER".equalsIgnoreCase(provider);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. `nid.naver.com`의 토큰 발급 엔드포인트로 POST 요청
     * 2. client_id, secret, code, state 파라미터 전송
     * 3. 응답 JSON에서 `access_token` 추출
     *
     * @throws AuthException 토큰 발급 실패 또는 응답값 누락 시
     */
    @Override
    public String getAccessToken(String code) {
        log.info("네이버 액세스 토큰 요청");
        Map response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nid.naver.com")
                        .path("/oauth2.0/token")
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("code", code)
                        .queryParam("state", "test_state") // 상태 토큰은 임의 설정
                        .queryParam("redirect_uri", redirectUri)
                        .build())
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
     * 1. `openapi.naver.com`의 회원정보 조회 API 호출 (Authorization Header 포함)
     * 2. 응답 JSON 내부의 `response` 객체 파싱
     * 3. 이메일, 이름, 프로필 이미지를 공통 Map 구조로 변환하여 반환
     */
    @Override
    public Map<String, Object> getUserProfile(String accessToken) {
        log.info("네이버 유저 프로필 요청");
        Map response = webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> responseMap = (Map<String, Object>) response.get("response");

        Map<String, Object> result = new HashMap<>();
        result.put("email", responseMap.get("email"));
        result.put("name", responseMap.get("name"));
        result.put("profileUrl", responseMap.get("profile_image"));
        return result;
    }
}