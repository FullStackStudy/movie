package com.movie.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@RequiredArgsConstructor
public class RedisNotifyConfig {
    private final RedisConnectionFactory redisConnectionFactory;

    @PostConstruct
    public void configureNotifyKeyspaceEvents() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            // notify-keyspace-events 설정 (예: 만료 이벤트 수신 위해 Ex 설정)
            connection.setConfig("notify-keyspace-events", "Ex");
            System.out.println("Redis notify-keyspace-events 설정 완료");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Redis notify-keyspace-events 설정 실패");
        }
    }
}
