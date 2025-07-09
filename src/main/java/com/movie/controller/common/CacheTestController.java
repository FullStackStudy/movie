package com.movie.controller.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.movie.dto.movie.MovieDto;
import com.movie.service.movie.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cache-test")
@RequiredArgsConstructor
@Slf4j
public class CacheTestController {

    private final CacheManager cacheManager;
    private final MovieService movieService;

    // 캐시 상태 상세 조회
    @GetMapping("/status")
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheInfo = new HashMap<>();
                
                // Caffeine 캐시 정보
                if (cache.getNativeCache() instanceof Cache) {
                    Cache<Object, Object> caffeineCache = (Cache<Object, Object>) cache.getNativeCache();
                    CacheStats stats = caffeineCache.stats();
                    
                    cacheInfo.put("estimatedSize", caffeineCache.estimatedSize());
                    cacheInfo.put("hitCount", stats.hitCount());
                    cacheInfo.put("missCount", stats.missCount());
                    cacheInfo.put("hitRate", stats.hitRate());
                    cacheInfo.put("loadSuccessCount", stats.loadSuccessCount());
                    cacheInfo.put("loadFailureCount", stats.loadFailureCount());
                    cacheInfo.put("totalLoadTime", stats.totalLoadTime());
                    cacheInfo.put("evictionCount", stats.evictionCount());
                }
                
                status.put(cacheName, cacheInfo);
            }
        });
        
        return status;
    }

    // 메인 영화 캐시 테스트
    @GetMapping("/test-main-movies")
    public Map<String, Object> testMainMoviesCache() {
        Map<String, Object> result = new HashMap<>();
        
        long startTime = System.currentTimeMillis();
        List<MovieDto> movies = movieService.getMainMovies();
        long endTime = System.currentTimeMillis();
        
        result.put("executionTime", endTime - startTime + "ms");
        result.put("movieCount", movies.size());
        result.put("movies", movies);
        
        log.info("메인 영화 조회 완료: {}ms, {}개 영화", endTime - startTime, movies.size());
        
        return result;
    }

    // 영화 검색 캐시 테스트
    @GetMapping("/test-search")
    public Map<String, Object> testSearchCache(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        long startTime = System.currentTimeMillis();
        List<MovieDto> movies = movieService.searchMovies(keyword);
        long endTime = System.currentTimeMillis();
        
        result.put("keyword", keyword);
        result.put("executionTime", endTime - startTime + "ms");
        result.put("movieCount", movies.size());
        result.put("movies", movies);
        
        log.info("영화 검색 완료: {}ms, 키워드: {}, {}개 영화", endTime - startTime, keyword, movies.size());
        
        return result;
    }

    // 캐시 키 목록 조회
    @GetMapping("/keys")
    public Map<String, Object> getCacheKeys() {
        Map<String, Object> keys = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof Cache) {
                Cache<Object, Object> caffeineCache = (Cache<Object, Object>) cache.getNativeCache();
                Map<Object, Object> cacheKeys = new HashMap<>();
                
                caffeineCache.asMap().forEach((key, value) -> {
                    cacheKeys.put(key.toString(), value.getClass().getSimpleName());
                });
                
                keys.put(cacheName, cacheKeys);
            }
        });
        
        return keys;
    }

    // 캐시 성능 테스트
    @GetMapping("/performance-test")
    public Map<String, Object> performanceTest() {
        Map<String, Object> result = new HashMap<>();
        
        // 첫 번째 호출 (캐시 미스)
        long startTime1 = System.currentTimeMillis();
        List<MovieDto> movies1 = movieService.getMainMovies();
        long endTime1 = System.currentTimeMillis();
        long firstCall = endTime1 - startTime1;
        
        // 두 번째 호출 (캐시 히트)
        long startTime2 = System.currentTimeMillis();
        List<MovieDto> movies2 = movieService.getMainMovies();
        long endTime2 = System.currentTimeMillis();
        long secondCall = endTime2 - startTime2;
        
        result.put("firstCallTime", firstCall + "ms");
        result.put("secondCallTime", secondCall + "ms");
        result.put("performanceImprovement", String.format("%.2f%%", (double)(firstCall - secondCall) / firstCall * 100));
        result.put("movieCount", movies1.size());
        
        log.info("성능 테스트 완료: 첫 번째 호출 {}ms, 두 번째 호출 {}ms, 성능 향상 {}%", 
                firstCall, secondCall, (double)(firstCall - secondCall) / firstCall * 100);
        
        return result;
    }
} 