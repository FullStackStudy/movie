package com.movie.service.recommend;

import com.movie.dto.recommand.RecommendResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendRedisService {
    private RedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL = Duration.ofHours(1);


}
