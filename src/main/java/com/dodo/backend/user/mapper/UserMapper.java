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

    void updateUserRegistrationInfo(User user);
}
