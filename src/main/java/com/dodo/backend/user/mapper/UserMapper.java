package com.dodo.backend.user.mapper;

import com.dodo.backend.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * 사용자(User) 도메인의 복잡한 조회 및 동적 쿼리를 담당하는 MyBatis Mapper 인터페이스입니다.
 * <p>
 * 기본적인 CRUD(생성, 조회, 수정, 삭제)는 JPA 기반의 {@link com.dodo.backend.user.repository.UserRepository}를 우선 사용된다.
 */
@Mapper
public interface UserMapper {

    /**
     * 신규 가입 유저의 추가 정보(닉네임, 지역 등)를 업데이트하고 계정을 활성화 상태로 변경합니다.
     * <p>
     * 소셜 로그인 이후 'REGISTER' 상태인 유저가 필수 정보를 모두 입력했을 때 호출되며,
     * 엔티티의 필드 값을 기반으로 DB 레코드를 갱신합니다.
     *
     * @param user 추가 정보가 포함된 유저 엔티티 객체
     */
    void updateUserRegistrationInfo(User user);

    /**
     * 유저의 계정 상태(UserStatus)를 명시적으로 변경합니다.
     * <p>
     * 주로 회원 탈퇴(DELETED), 계정 정지(SUSPENDED) 등 특정 상태 값만
     * 빠르게 업데이트해야 하는 비즈니스 로직에서 사용됩니다.
     *
     * @param userId 유저의 고유 식별자 (UUID)
     * @param status 변경하고자 하는 새로운 유저 상태값
     */
    void updateUserStatus(@Param("userId") UUID userId, @Param("status") String status);
}
