package com.dodo.backend.user.service;

import com.dodo.backend.auth.exception.AuthErrorCode;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.user.dto.request.UserRequest.UserRegisterRequest;
import com.dodo.backend.user.dto.response.UserResponse.UserRegisterResponse;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.exception.UserErrorCode;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link UserServiceImpl}의 비즈니스 로직을 검증하는 통합 테스트 클래스입니다.
 * <p>
 * 소셜 유저 저장 및 조회(findOrSaveSocialUser)와
 * 추가 정보 입력(registerAdditionalInfo) 로직을 검증합니다.
 */
@SpringBootTest
@Transactional
@Slf4j
@TestPropertySource(properties = {
        "naver.client_id=test_naver_id",
        "naver.client_secret=test_naver_secret",
        "naver.redirect_uri=http://localhost:8080/login/oauth2/code/naver",
        "google.client_id=test_google_id",
        "google.client_secret=test_google_secret",
        "google.redirect_uri=http://localhost:8080/login/oauth2/code/google",
        "jwt.secret=test_jwt_secret_key_must_be_very_long_to_pass_validation_check"
})
class UserServiceImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private WebClient webClient;

    @Nested
    @DisplayName("소셜 유저 조회 및 저장 테스트 (findOrSaveSocialUser)")
    class FindOrSaveSocialUserTest {

        /**
         * DB에 없는 새로운 이메일로 요청 시, REGISTER 상태로 저장되고 신규 회원 플래그가 true여야 합니다.
         */
        @Test
        @DisplayName("신규 유저: DB에 REGISTER 상태로 저장되고 isNewMember=true 반환")
        void newMemberRegistrationTest() {
            // given
            String email = "new_user@test.com";
            String name = "신규유저";
            log.info("신규 회원 가입 테스트 시작 - 이메일: {}", email);

            // when
            Map<String, Object> result = userService.findOrSaveSocialUser(email, name, "");
            log.info("서비스 실행 결과: {}", result);

            // then
            assertThat(result.get("isNewMember")).isEqualTo(true);
            assertThat(result.get("email")).isEqualTo(email);

            User savedUser = userRepository.findByEmail(email).orElse(null);
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.REGISTER);

            log.info("DB 저장 상태 확인 완료 - 상태: {}", savedUser.getUserStatus());
        }

        /**
         * 이미 ACTIVE 상태인 유저가 요청 시, isNewMember=false와 기존 정보를 반환해야 합니다.
         */
        @Test
        @DisplayName("기존 유저(ACTIVE): 정보 반환 및 isNewMember=false 반환")
        void existingActiveMemberTest() {
            // given
            String email = "active@test.com";
            User activeUser = createTestUser(email, UserStatus.ACTIVE);
            userRepository.save(activeUser);
            log.info("기존 ACTIVE 유저 저장 완료: {}", email);

            // when
            Map<String, Object> result = userService.findOrSaveSocialUser(email, "기존유저", "");
            log.info("서비스 실행 결과: {}", result);

            // then
            assertThat(result.get("isNewMember")).isEqualTo(false);
            assertThat(result.get("userId")).isEqualTo(activeUser.getUsersId());
            log.info("기존 유저 ID 일치 확인 완료.");
        }

        /**
         * 정지된(SUSPENDED) 계정으로 접근 시 AuthException이 발생해야 합니다.
         */
        @Test
        @DisplayName("정지된 계정(SUSPENDED): 로그인 시도 시 AuthException 발생")
        void suspendedUserTest() {
            // given
            String email = "suspended@test.com";
            User suspendedUser = createTestUser(email, UserStatus.SUSPENDED);
            userRepository.save(suspendedUser);
            log.info("정지된(SUSPENDED) 유저 저장 완료: {}", email);

            // when & then
            AuthException exception = assertThrows(AuthException.class, () -> {
                userService.findOrSaveSocialUser(email, "정지유저", "");
            });

            log.info("예상된 예외 발생 확인: {}", exception.getErrorCode());
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_RESTRICTED);
        }
    }

    @Nested
    @DisplayName("추가 정보 입력 및 가입 완료 테스트 (registerAdditionalInfo)")
    class RegisterAdditionalInfoTest {

        /**
         * REGISTER 상태의 유저가 올바른 정보를 입력하면 ACTIVE 상태로 변경되고 토큰이 발급되어야 합니다.
         */
        @Test
        @DisplayName("정상 가입: REGISTER 상태 유저가 정보 입력 시 ACTIVE로 변경되고 토큰 발급")
        void completeRegistrationSuccessTest() {
            // given
            String email = "register@test.com";
            User registerUser = createTestUser(email, UserStatus.REGISTER);
            userRepository.save(registerUser);

            em.flush();
            em.clear();

            log.info("가입 대기(REGISTER) 유저 준비 완료: {}", email);

            UserRegisterRequest request = UserRegisterRequest.builder()
                    .nickname("멋진닉네임")
                    .region("Seoul")
                    .hasFamily(true)
                    .build();

            // when
            UserRegisterResponse response = userService.registerAdditionalInfo(request, email);
            log.info("회원가입 완료 응답 메시지: {}", response.getMessage());

            em.clear();

            // then
            User updatedUser = userRepository.findByEmail(email).orElseThrow();
            log.info("조회된 유저 상태: {}", updatedUser.getUserStatus());

            assertThat(updatedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE); // 이제 정상 통과!
            assertThat(updatedUser.getNickname()).isEqualTo("멋진닉네임");
            assertThat(updatedUser.getHasFamily()).isTrue();

            log.info("유저 상태 ACTIVE 변경 확인 완료.");
        }

        /**
         * 이미 존재하는 닉네임으로 가입을 시도하면 UserException(NICKNAME_DUPLICATED)이 발생해야 합니다.
         */
        @Test
        @DisplayName("중복 닉네임: 이미 존재하는 닉네임으로 가입 시도시 UserException 발생")
        void duplicatedNicknameTest() {
            // given
            String existingNick = "중복닉네임";

            User existingUser = User.builder()
                    .email("existing@test.com")
                    .name("기존유저")
                    .nickname(existingNick)
                    .region("Busan")
                    .hasFamily(false)
                    .role(UserRole.USER)
                    .userStatus(UserStatus.ACTIVE)
                    .userCreatedAt(LocalDateTime.now())
                    .notificationEnabled(true)
                    .profileUrl("")
                    .build();
            userRepository.save(existingUser);
            log.info("중복 닉네임({})을 가진 기존 유저 저장 완료", existingNick);

            String newEmail = "new@test.com";
            User newUser = createTestUser(newEmail, UserStatus.REGISTER);
            userRepository.save(newUser);

            UserRegisterRequest request = UserRegisterRequest.builder()
                    .nickname(existingNick)
                    .region("Seoul")
                    .hasFamily(false)
                    .build();

            // when & then
            UserException exception = assertThrows(UserException.class, () -> {
                userService.registerAdditionalInfo(request, newEmail);
            });

            log.info("중복 닉네임 예외 발생 확인: {}", exception.getErrorCode());
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.NICKNAME_DUPLICATED);
        }

        /**
         * 이미 ACTIVE 상태인 유저가 추가 정보 입력을 시도하면 UserException(INVALID_REQUEST)이 발생해야 합니다.
         */
        @Test
        @DisplayName("잘못된 상태: 이미 ACTIVE인 유저가 가입 시도시 UserException 발생")
        void invalidStatusTest() {
            // given
            String email = "already_active@test.com";
            User activeUser = createTestUser(email, UserStatus.ACTIVE);
            userRepository.save(activeUser);
            log.info("이미 가입된(ACTIVE) 유저 준비 완료: {}", email);

            UserRegisterRequest request = UserRegisterRequest.builder()
                    .nickname("새닉네임")
                    .region("Seoul")
                    .hasFamily(true)
                    .build();

            // when & then
            UserException exception = assertThrows(UserException.class, () -> {
                userService.registerAdditionalInfo(request, email);
            });

            log.info("잘못된 상태 접근 예외 발생 확인: {}", exception.getErrorCode());
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * 테스트 헬퍼 메서드: 특정 상태를 가진 유저 엔티티 생성
     */
    private User createTestUser(String email, UserStatus status) {
        return User.builder()
                .email(email)
                .name("테스트유저")
                .nickname(status == UserStatus.REGISTER ? "" : "Nick_" + email.split("@")[0])
                .profileUrl("")
                .region("Seoul")
                .notificationEnabled(true)
                .role(UserRole.USER)
                .userStatus(status)
                .userCreatedAt(LocalDateTime.now())
                .hasFamily(false)
                .build();
    }
}