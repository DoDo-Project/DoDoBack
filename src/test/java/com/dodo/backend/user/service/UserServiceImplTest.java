package com.dodo.backend.user.service;

import com.dodo.backend.auth.exception.AuthErrorCode;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.user.dto.request.UserRequest.UserRegisterRequest;
import com.dodo.backend.user.dto.request.UserRequest.UserUpdateRequest;
import com.dodo.backend.user.dto.response.UserResponse;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 유저 서비스 레이어의 비즈니스 로직을 검증하는 통합 테스트 클래스입니다.
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
    @DisplayName("소셜 유저 조회 및 저장 테스트")
    class FindOrSaveSocialUserTest {

        @Test
        @DisplayName("신규 유저는 REGISTER 상태로 저장")
        void newMemberRegistrationTest() {
            // given
            String email = "new_user@test.com";
            String name = "신규유저";
            log.info("신규 유저 저장 테스트 시작 - 이메일: {}", email);

            // when
            Map<String, Object> result = userService.findOrSaveSocialUser(email, name, "");

            // then
            assertThat(result.get("isNewMember")).isEqualTo(true);
            User savedUser = userRepository.findByEmail(email).orElseThrow();
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.REGISTER);
            log.info("신규 유저 가입 대기 상태 저장 확인 완료");
        }

        @Test
        @DisplayName("정지된 계정은 접근 제한 예외 발생")
        void suspendedUserTest() {
            // given
            String email = "suspended@test.com";
            User suspendedUser = createTestUser(email, UserStatus.SUSPENDED);
            userRepository.save(suspendedUser);
            log.info("정지 계정 로그인 차단 테스트 시작 - 이메일: {}", email);

            // when & then
            AuthException exception = assertThrows(AuthException.class, () -> {
                userService.findOrSaveSocialUser(email, "정지유저", "");
            });

            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_RESTRICTED);
            log.info("정지 계정 로그인 차단 확인 완료");
        }
    }

    @Nested
    @DisplayName("추가 정보 입력 및 가입 완료 테스트")
    class RegisterAdditionalInfoTest {

        @Test
        @DisplayName("정보 입력 완료 시 ACTIVE 상태로 변경")
        void completeRegistrationSuccessTest() {
            // given
            String email = "register@test.com";
            userRepository.save(createTestUser(email, UserStatus.REGISTER));
            em.flush();
            em.clear();
            log.info("추가 정보 입력 테스트 시작 - 이메일: {}", email);

            UserRegisterRequest request = UserRegisterRequest.builder()
                    .nickname("멋진닉네임")
                    .region("서울")
                    .hasFamily(true)
                    .build();

            // when
            userService.registerAdditionalInfo(request, email);

            // then
            em.clear();
            User updatedUser = userRepository.findByEmail(email).orElseThrow();
            assertThat(updatedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
            log.info("유저 상태 활성화 및 데이터 반영 성공 확인");
        }

        @Test
        @DisplayName("중복 닉네임 사용 시 예외 발생")
        void duplicatedNicknameTest() {
            // given
            String existingNick = "중복닉네임";
            User existingUser = User.builder()
                    .email("existing@test.com")
                    .name("기존유저")
                    .nickname(existingNick)
                    .region("서울") // region 필수값 추가
                    .userStatus(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .notificationEnabled(true)
                    .profileUrl("")
                    .userCreatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(existingUser);

            String newEmail = "new@test.com";
            userRepository.save(createTestUser(newEmail, UserStatus.REGISTER));
            log.info("닉네임 중복 가입 차단 테스트 시작 - 닉네임: {}", existingNick);

            UserRegisterRequest request = UserRegisterRequest.builder()
                    .nickname(existingNick)
                    .region("서울")
                    .hasFamily(false)
                    .build();

            // when & then
            UserException exception = assertThrows(UserException.class, () -> {
                userService.registerAdditionalInfo(request, newEmail);
            });

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.NICKNAME_DUPLICATED);
            log.info("닉네임 중복 가입 차단 확인 완료");
        }
    }

    @Nested
    @DisplayName("유저 정보 수정 테스트")
    class UpdateUserInfoTest {

        @Test
        @DisplayName("선택적 필드 수정 시 요청 데이터만 변경되고 응답에 반영됨")
        void updateUserInfoPartialSuccessTest() {
            // given
            String email = "update@test.com";
            User user = User.builder()
                    .email(email)
                    .name("백현빈")
                    .nickname("초기닉네임")
                    .region("서울")
                    .hasFamily(true)
                    .userStatus(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .notificationEnabled(true)
                    .profileUrl("url")
                    .userCreatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
            em.flush();
            em.clear();
            log.info("유저 정보 선택적 수정 테스트 시작");

            UserUpdateRequest request = UserUpdateRequest.builder()
                    .nickname("변경닉네임")
                    .region("부산")
                    .hasFamily(null)
                    .build();

            // when
            UserResponse.UserUpdateResponse response = userService.updateUserInfo(user.getUsersId(), request);

            // then
            assertThat(response.getNickname()).isEqualTo("변경닉네임");
            assertThat(response.getRegion()).isEqualTo("부산");
            assertThat(response.getHasFamily()).isTrue();

            em.flush();
            em.clear();
            User updatedUser = userRepository.findById(user.getUsersId()).orElseThrow();
            assertThat(updatedUser.getNickname()).isEqualTo("변경닉네임");
            log.info("필드 선택적 수정 및 기존 값 유지 확인 완료");
        }

        @Test
        @DisplayName("이미 존재하는 닉네임으로 수정 시도 시 UserException 발생")
        void updateUserInfoDuplicateNicknameTest() {
            // given
            String takenNickname = "이미있는닉네임";
            User otherUser = User.builder()
                    .email("other@test.com")
                    .name("기존유저")
                    .nickname(takenNickname)
                    .region("서울") // region 필수값 추가
                    .userStatus(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .notificationEnabled(true)
                    .profileUrl("")
                    .userCreatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(otherUser);

            User targetUser = createTestUser("target@test.com", UserStatus.ACTIVE);
            userRepository.save(targetUser);
            em.flush();
            em.clear();
            log.info("수정 시 닉네임 중복 차단 테스트 시작");

            UserUpdateRequest request = UserUpdateRequest.builder()
                    .nickname(takenNickname)
                    .build();

            // when & then
            UserException exception = assertThrows(UserException.class, () -> {
                userService.updateUserInfo(targetUser.getUsersId(), request);
            });

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.NICKNAME_DUPLICATED);
            log.info("수정 시 닉네임 중복 차단 확인 완료");
        }

        @Test
        @DisplayName("본인의 현재 닉네임 유지 시 예외 없이 수정 성공")
        void updateUserInfoSameNicknameSuccessTest() {
            // given
            String myNickname = "나의닉네임";
            User user = User.builder()
                    .email("me@test.com")
                    .name("본인")
                    .nickname(myNickname)
                    .region("서울")
                    .userStatus(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .notificationEnabled(true)
                    .profileUrl("")
                    .userCreatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
            em.flush();
            em.clear();
            log.info("본인 닉네임 유지 수정 테스트 시작");

            UserUpdateRequest request = UserUpdateRequest.builder()
                    .nickname(myNickname)
                    .region("인천")
                    .build();

            // when
            UserResponse.UserUpdateResponse response = userService.updateUserInfo(user.getUsersId(), request);

            // then
            assertThat(response.getNickname()).isEqualTo(myNickname);
            assertThat(response.getRegion()).isEqualTo("인천");
            log.info("본인 닉네임 유지 수정 성공 확인");
        }
    }

    private User createTestUser(String email, UserStatus status) {
        return User.builder()
                .email(email)
                .name("테스트유저")
                .nickname(status == UserStatus.REGISTER ? "" : "Nick_" + UUID.randomUUID().toString().substring(0, 8))
                .profileUrl("")
                .region("서울")
                .notificationEnabled(true)
                .role(UserRole.USER)
                .userStatus(status)
                .userCreatedAt(LocalDateTime.now())
                .hasFamily(false)
                .build();
    }
}