package com.dodo.backend.user.service;

import com.dodo.backend.auth.exception.AuthErrorCode;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * {@link UserServiceImpl}의 비즈니스 로직을 검증하는 통합 테스트 클래스입니다.
 * <p>
 * 사용자 계정 상태에 따른 접근 제어 및 신규 회원 가입 프로세스가
 * DB 및 외부 API 모킹 환경에서 정상적으로 동작하는지 확인합니다.
 */
@SpringBootTest
@Transactional
@Slf4j
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private WebClient webClient;

    /**
     * 계정 상태가 SUSPENDED(정지)인 경우 로그인이 차단되는지 검증합니다.
     */
    @Test
    @DisplayName("정지된 계정 로그인 시 AuthException 발생 테스트")
    void getNaverUserProfile_SuspendedErrorTest() {
        // given
        String email = "suspended@test.com";
        User suspendedUser = createTestUser(email, UserStatus.SUSPENDED);
        userRepository.save(suspendedUser);
        log.info("Saved suspended user for testing: email={}", email);

        setupWebClientMock(email, "정지유저");

        // when & then
        AuthException exception = assertThrows(AuthException.class, () -> {
            userService.getNaverUserProfile("mock-token");
        });

        log.info("Caught expected exception for suspended user. ErrorCode: {}", exception.getErrorCode());
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_RESTRICTED);
    }

    /**
     * 계정 상태가 DORMANT(휴면)인 경우 로그인이 차단되는지 검증합니다.
     */
    @Test
    @DisplayName("휴면 계정 로그인 시 AuthException 발생 테스트")
    void getNaverUserProfile_DormantErrorTest() {
        // given
        String email = "dormant@test.com";
        User dormantUser = createTestUser(email, UserStatus.DORMANT);
        userRepository.save(dormantUser);
        log.info("Saved dormant user for testing: email={}", email);

        setupWebClientMock(email, "휴면유저");

        // when & then
        AuthException exception = assertThrows(AuthException.class, () -> {
            userService.getNaverUserProfile("mock-token");
        });

        log.info("Caught expected exception for dormant user. ErrorCode: {}", exception.getErrorCode());
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_RESTRICTED);
    }

    /**
     * 계정 상태가 DELETED(삭제)인 경우 로그인이 차단되는지 검증합니다.
     */
    @Test
    @DisplayName("삭제된 계정 로그인 시 AuthException 발생 테스트")
    void getNaverUserProfile_DeletedErrorTest() {
        // given
        String email = "deleted@test.com";
        User deletedUser = createTestUser(email, UserStatus.DELETED);
        userRepository.save(deletedUser);
        log.info("Saved deleted user for testing: email={}", email);

        setupWebClientMock(email, "삭제유저");

        // when & then
        AuthException exception = assertThrows(AuthException.class, () -> {
            userService.getNaverUserProfile("mock-token");
        });

        log.info("Caught expected exception for deleted user. ErrorCode: {}", exception.getErrorCode());
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_RESTRICTED);
    }

    /**
     * 신규 회원 가입 시 DB에 REGISTER 상태로 정상 저장되는지 검증합니다.
     */
    @Test
    @DisplayName("신규 회원 로그인 시 가입 대기 상태로 저장 테스트")
    void getNaverUserProfile_NewMemberRegistrationTest() {
        // given
        String email = "new_user@test.com";
        String name = "신규유저";
        setupWebClientMock(email, name);
        log.info("Initiating login for new member: email={}, name={}", email, name);

        // when
        Map<String, Object> result = userService.getNaverUserProfile("mock-token");
        log.info("Service result for new member: {}", result);

        // then
        assertThat(result.get("isNewMember")).isEqualTo(true);
        assertThat(result.get("email")).isEqualTo(email);

        User savedUser = userRepository.findByEmail(email).orElse(null);
        log.info("Verified saved user in DB. Email: {}, Status: {}",
                savedUser != null ? savedUser.getEmail() : "null",
                savedUser != null ? savedUser.getUserStatus() : "null");

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo(name);
        assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.REGISTER);
        assertThat(savedUser.getNickname()).isEmpty();
    }

    /**
     * 테스트용 유저 엔티티를 생성하는 헬퍼 메서드입니다.
     */
    private User createTestUser(String email, UserStatus status) {
        return User.builder()
                .email(email)
                .name("테스트유저")
                .nickname("test_" + status.name().toLowerCase())
                .profileUrl("")
                .region("Seoul")
                .notificationEnabled(true)
                .role(UserRole.USER)
                .userStatus(status)
                .userCreatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * WebClient의 API 응답을 시뮬레이션하기 위한 모킹 설정 메서드입니다.
     */
    private void setupWebClientMock(String email, String name) {
        Map<String, Object> mockResponse = Map.of(
                "response", Map.of("email", email, "name", name)
        );

        var uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        var headersSpec = mock(WebClient.RequestHeadersSpec.class);
        var responseSpec = mock(WebClient.ResponseSpec.class);

        given(webClient.get()).willReturn(uriSpec);
        given(uriSpec.uri(anyString())).willReturn(headersSpec);
        given(headersSpec.header(anyString(), anyString())).willReturn(headersSpec);
        given(headersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.bodyToMono(Map.class)).willReturn(Mono.just(mockResponse));
    }
}