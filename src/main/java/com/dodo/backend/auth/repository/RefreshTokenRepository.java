package com.dodo.backend.auth.repository;

import com.dodo.backend.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * 리프레시 토큰(Refresh Token) 엔티티의 데이터 접근 및 관리를 담당하는 리포지토리 인터페이스입니다.
 * <p>
 * Redis의 {@code refresh_token} 해시에 저장된 데이터를 처리합니다.
 */
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    /**
     * 주어진 리프레시 토큰(Refresh Token) 값을 기준으로 저장된 엔티티 정보를 조회합니다.
     * <p>
     * {@code @Indexed}가 적용된 토큰 필드를 사용하여 데이터를 검색합니다.
     */
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
