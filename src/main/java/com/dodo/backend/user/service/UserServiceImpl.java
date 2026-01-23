package com.dodo.backend.user.service;

import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.dodo.backend.auth.exception.AuthErrorCode.ACCOUNT_RESTRICTED;
import static com.dodo.backend.user.entity.UserStatus.*;

/**
 * {@link UserService}의 구현체로, 네이버 Open API와 연동하여 유저 정보를 처리합니다.
 * <p>
 * {@link UserRepository}를 통해 회원 가입 여부를 확인하고,
 * 정지/휴면 등 계정 상태에 따른 비즈니스 예외 처리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final WebClient webClient;
    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. {@link WebClient}를 통해 네이버 프로필 API 호출
     * 2. 수신된 JSON 응답에서 이메일, 이름, 프로필 이미지 추출
     * 3. {@code email}을 기준으로 기존 가입 여부 조회
     * 4. 기존 회원일 경우: 계정 상태(정지/삭제/휴면)를 체크하여 예외 또는 유저 데이터 반환
     * 5. 신규 회원일 경우: 기본 정보를 {@code User} 엔티티로 생성하여 DB에 저장
     *
     * @throws AuthException 유저 상태가 서비스 이용 제한(SUSPENDED, DORMANT, DELETED)인 경우
     */
    @Transactional
    @Override
    public Map<String, Object> getNaverUserProfile(String accessToken) {

        Map response = webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> responseMap = (Map<String, Object>) response.get("response");
        String email = (String) responseMap.get("email");
        String name = (String) responseMap.get("name");
        String profileImage = (String) responseMap.get("profile_image");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("email", email);
        resultMap.put("name", name);
        resultMap.put("profileUrl", profileImage != null ? profileImage : "");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {

            if (user.getUserStatus() == SUSPENDED
                    || user.getUserStatus() == DORMANT
                    || user.getUserStatus() == DELETED) {
                throw new AuthException(ACCOUNT_RESTRICTED);
            }

            if (user.getUserStatus() == UserStatus.REGISTER) {
                resultMap.put("isNewMember", true);
            } else {
                resultMap.put("isNewMember", false);
                resultMap.put("userId", user.getUsersId());
                resultMap.put("role", user.getRole().name());
            }

        } else {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .profileUrl(profileImage != null ? profileImage : "")
                    .role(UserRole.USER)
                    .userStatus(UserStatus.REGISTER)
                    .userCreatedAt(LocalDateTime.now())
                    .nickname("")
                    .region("")
                    .notificationEnabled(true)
                    .build();

            userRepository.save(newUser);

            resultMap.put("isNewMember", true);
        }

        return resultMap;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. {@link WebClient}를 통해 구글 프로필 API 호출
     * 2. 수신된 JSON 응답에서 이메일, 이름, 프로필 이미지 추출
     * 3. {@code email}을 기준으로 기존 가입 여부 조회
     * 4. 기존 회원일 경우: 계정 상태(정지/삭제/휴면)를 체크하여 예외 또는 유저 데이터 반환
     * 5. 신규 회원일 경우: 기본 정보를 {@code User} 엔티티로 생성하여 DB에 저장
     *
     * @throws AuthException 유저 상태가 서비스 이용 제한(SUSPENDED, DORMANT, DELETED)인 경우
     */
    @Transactional
    @Override
    public Map<String, Object> getGoogleUserProfile(String accessToken) {

        Map response = webClient.get()
                .uri("https://www.googleapis.com/oauth2/v3/userinfo") // 구글 엔드포인트
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String email = (String) response.get("email");
        String name = (String) response.get("name");
        String profileImage = (String) response.get("picture");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("email", email);
        resultMap.put("name", name);
        resultMap.put("profileUrl", profileImage != null ? profileImage : "");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {

            if (user.getUserStatus() == SUSPENDED
                    || user.getUserStatus() == DORMANT
                    || user.getUserStatus() == DELETED) {
                throw new AuthException(ACCOUNT_RESTRICTED);
            }

            if (user.getUserStatus() == UserStatus.REGISTER) {
                resultMap.put("isNewMember", true);
            } else {
                resultMap.put("isNewMember", false);
                resultMap.put("userId", user.getUsersId());
                resultMap.put("role", user.getRole().name());
            }

        } else {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .profileUrl(profileImage != null ? profileImage : "")
                    .role(UserRole.USER)
                    .userStatus(UserStatus.REGISTER)
                    .userCreatedAt(LocalDateTime.now())
                    .nickname("")
                    .region("")
                    .notificationEnabled(true)
                    .build();

            userRepository.save(newUser);

            resultMap.put("isNewMember", true);
        }

        return resultMap;
    }
}