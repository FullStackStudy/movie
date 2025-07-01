package com.movie.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final CacheManager cacheManager;
    private final MovieService movieService;

    // 캐시 통계 조회
    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("name", cacheName);
                cacheInfo.put("nativeCache", cache.getNativeCache());
                stats.put(cacheName, cacheInfo);
            }
        });
        
        return stats;
    }

    // 특정 캐시 삭제
    @DeleteMapping("/{cacheName}")
    public String evictCache(@PathVariable String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("캐시 삭제 완료: {}", cacheName);
            return "캐시 삭제 완료: " + cacheName;
        }
        return "캐시를 찾을 수 없습니다: " + cacheName;
    }

    // 모든 캐시 삭제
    @DeleteMapping("/all")
    public String evictAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        log.info("모든 캐시 삭제 완료");
        return "모든 캐시 삭제 완료";
    }

    // 메인 영화 캐시 삭제
    @DeleteMapping("/main-movies")
    public String evictMainMoviesCache() {
        movieService.evictMainMoviesCache();
        return "메인 영화 캐시 삭제 완료";
    }

    // 검색 캐시 삭제
    @DeleteMapping("/search")
    public String evictSearchCache() {
        movieService.evictSearchCache();
        return "검색 캐시 삭제 완료";
    }
} 