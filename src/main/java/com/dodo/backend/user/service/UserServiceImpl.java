package com.dodo.backend.user.service;

import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.common.util.JwtTokenProvider;
import com.dodo.backend.user.dto.request.UserRequest.UserRegisterRequest;
import com.dodo.backend.user.dto.response.UserResponse.UserRegisterResponse;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.mapper.UserMapper;
import com.dodo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.dodo.backend.auth.exception.AuthErrorCode.ACCOUNT_RESTRICTED;
import static com.dodo.backend.user.dto.response.UserResponse.UserRegisterResponse.toDto;
import static com.dodo.backend.user.entity.UserStatus.*;
import static com.dodo.backend.user.exception.UserErrorCode.*;

/**
 * {@link UserService}의 구현체로, 유저 도메인의 비즈니스 로직을 수행합니다.
 * <p>
 * 소셜 로그인 직후의 유저 상태 판별 및 저장,
 * 추가 정보 입력을 통한 회원가입 완료 처리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 이메일을 기준으로 DB에서 유저 조회
     * 2. (기존 유저) 계정 제재 상태(정지/삭제 등) 검증 및 가입 여부 확인
     * 3. (신규 유저) 'REGISTER' 상태의 유저 엔티티 생성 및 저장
     *
     * @return 신규 회원 여부(isNewMember), 유저 식별자, 권한 등을 포함한 Map
     * @throws AuthException 계정이 정지, 휴면, 또는 삭제된 상태일 경우
     */
    @Transactional
    @Override
    public Map<String, Object> findOrSaveSocialUser(String email, String name, String profileImage) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("email", email);
        resultMap.put("name", name);
        resultMap.put("profileUrl", profileImage != null ? profileImage : "");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            log.info("기존 등록된 유저 확인 - 상태: {}", user.getUserStatus());
            validateUserStatus(user);

            if (user.getUserStatus() == UserStatus.REGISTER) {
                resultMap.put("isNewMember", true);
            } else {
                resultMap.put("isNewMember", false);
                resultMap.put("userId", user.getUsersId());
                resultMap.put("role", user.getRole().name());
            }

        } else {
            log.info("가입되지 않은 신규 유저 - 정보 생성 및 임시 저장 진행");
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
     * 유저의 계정 상태가 서비스 이용 가능한 상태인지 검증합니다.
     */
    private void validateUserStatus(User user) {
        if (user.getUserStatus() == SUSPENDED
                || user.getUserStatus() == DORMANT
                || user.getUserStatus() == DELETED) {
            log.warn("계정 사용 제한 유저 접속 시도 - 이메일: {}, 상태: {}", user.getEmail(), user.getUserStatus());
            throw new AuthException(ACCOUNT_RESTRICTED);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 유저 조회 및 'REGISTER' 상태(가입 대기) 검증
     * 2. 닉네임 중복 여부 확인 (비즈니스 로직)
     * 3. MyBatis를 통한 정보 업데이트 및 계정 활성화(ACTIVE)
     * 4. 정회원(USER) 권한의 새로운 Access/Refresh 토큰 발급
     * <p>
     * (참고: 입력값의 null/빈값 여부는 컨트롤러단에서 @Valid를 통해 사전에 검증됩니다.)
     *
     * @throws UserException 중복 닉네임, 잘못된 상태, 유저를 찾을 수 없는 경우
     */
    @Transactional
    @Override
    public UserRegisterResponse registerAdditionalInfo(UserRegisterRequest request, String email) {
        log.info("추가 정보 입력 시작");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        if (user.getUserStatus() != UserStatus.REGISTER) {
            throw new UserException(INVALID_REQUEST);
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new UserException(NICKNAME_DUPLICATED);
        }

        userMapper.updateUserRegistrationInfo(request.toEntity(email));

        String accessToken = jwtTokenProvider.createAccessToken(user.getUsersId(), "USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUsersId());

        return toDto(user, "회원가입 성공했습니다.",
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenValidityInMilliseconds());
    }
}