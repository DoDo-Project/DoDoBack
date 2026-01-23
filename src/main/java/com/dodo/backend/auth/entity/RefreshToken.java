package com.dodo.backend.auth.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;


/**
 * 사용자 인증 갱신을 위한 리프레시 토큰(Refresh Token) 정보를 관리하는 Redis 엔티티 클래스입니다.
 * <p>
 * Redis의 {@code refresh_token} 해시(Hash)와 매핑됩니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@RedisHash(value = "refresh_token", timeToLive = 1209600000)
public class RefreshToken {

    @Id
    private String usersId;

    @Indexed
    private String refreshToken;

    private String role;
}
