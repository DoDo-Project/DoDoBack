package com.dodo.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 인프라와 통신하기 위한 설정을 담당하는 클래스입니다.
 * <p>
 * 데이터 직렬화 방식을 설정하여 Redis 클라이언트에서 데이터를 읽기 쉽게 관리하며,
 * 리프레시 토큰 저장소(Repository) 기능을 활성화합니다.
 */
@Configuration
@EnableRedisRepositories
public class RedisConfig {

    /**
     * RedisTemplate을 커스텀 설정하여 데이터 직렬화 방식을 지정합니다.
     * <p>
     * Key는 문자열로, Value는 JSON 형식으로 저장되도록 설정합니다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return 설정이 완료된 RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return redisTemplate;
    }
}