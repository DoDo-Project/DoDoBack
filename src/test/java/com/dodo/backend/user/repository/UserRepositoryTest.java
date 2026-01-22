package com.dodo.backend.user.repository;

import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UserRepository}의 데이터 접근 로직(JPA)을 검증하는 테스트 클래스입니다.
 * <p>
 * 실제 DB(또는 H2)와 연결하여 엔티티의 저장(Save) 및 조회(Find) 기능이
 * 정상적으로 동작하는지 통합 테스트(SpringBootTest)를 통해 확인합니다.
 */
@SpringBootTest
@Transactional
@Slf4j
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    /**
     * 새로운 회원 엔티티가 데이터베이스에 정상적으로 Insert 되는지 검증합니다.
     * <p>
     * 저장 후 생성된 PK(ID)의 존재 여부와 저장된 상태 값(Status)을 확인합니다.
     */
    @Test
    @DisplayName("회원 저장 성공 테스트")
    void saveUserTest() {

        // given
        User user = User.builder()
                .email("test@naver.com")
                .name("테스트유저")
                .profileUrl("https://profile.com/img.png")
                .role(UserRole.USER)
                .userStatus(UserStatus.REGISTER)
                .userCreatedAt(LocalDateTime.now())
                .nickname("dodo")
                .region("Seoul")
                .notificationEnabled(true)
                .build();

        // when
        User savedUser = userRepository.save(user);

        log.info("Saved User Info: {}", savedUser);
        log.info("Saved User ID: {}", savedUser.getUsersId());

        // then
        assertThat(savedUser.getUsersId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@naver.com");
        assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.REGISTER);
    }

    /**
     * 이메일(Unique Key)을 기준으로 특정 회원을 정확히 조회해오는지 검증합니다.
     * <p>
     * 저장된 회원의 이름과 이메일이 조회 결과와 일치하는지 확인합니다.
     */
    @Test
    @DisplayName("이메일로 회원 조회 성공 테스트")
    void findByEmailTest() {

        // given
        User user = User.builder()
                .email("test@naver.com")
                .name("테스트유저")
                .profileUrl("https://profile.com/img.png")
                .role(UserRole.USER)
                .userStatus(UserStatus.REGISTER)
                .userCreatedAt(LocalDateTime.now())
                .nickname("dodo")
                .region("Seoul")
                .notificationEnabled(true)
                .build();

        userRepository.save(user);

        // when
        User foundUser = userRepository.findByEmail("test@naver.com")
                .orElse(null);

        log.info("Found User Info: {}", foundUser);

        // then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getName()).isEqualTo("테스트유저");
        assertThat(foundUser.getEmail()).isEqualTo("test@naver.com");
    }

    /**
     * 특정 회원 엔티티를 데이터베이스에서 삭제했을 때, 해당 데이터가 정상적으로 제거되는지 검증합니다.
     * <p>
     * 삭제 실행 후, 동일한 식별자(ID)로 조회 시 데이터가 존재하지 않음을 확인합니다.
     */
    @Test
    @DisplayName("회원 삭제 성공 테스트")
    void deleteUserTest() {
        // given
        User user = User.builder()
                .email("delete@test.com")
                .name("삭제대상")
                .profileUrl("https://profile.com")
                .role(UserRole.USER)
                .userStatus(UserStatus.REGISTER)
                .userCreatedAt(LocalDateTime.now())
                .nickname("deleteUser")
                .region("Seoul")
                .notificationEnabled(true)
                .build();
        User savedUser = userRepository.save(user);
        java.util.UUID targetId = savedUser.getUsersId();

        log.info("Before Delete - User ID: {}, Email: {}", targetId, savedUser.getEmail());

        // when
        userRepository.delete(savedUser);
        userRepository.flush();

        // then
        boolean isExists = userRepository.existsById(targetId);
        java.util.Optional<User> foundUser = userRepository.findById(targetId);

        log.info("After Delete - Target ID: {}", targetId);
        log.info("After Delete - Existence Status: {}", isExists);

        assertThat(isExists).isFalse();
        assertThat(foundUser).isEmpty();
    }
}