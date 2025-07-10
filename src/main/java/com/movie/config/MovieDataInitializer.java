package com.movie.config;

import com.movie.service.movie.MovieCrawlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieDataInitializer implements CommandLineRunner {

    private final MovieCrawlingService movieCrawlingService;
    // 어플리케이션이 실행될때 한번은 크롤링이 되게 하는 기능
    @Override
    public void run(String... args) throws Exception {
        log.info("🎬 애플리케이션 시작 - CGV 영화 정보 크롤링을 시작합니다...");
        
        try {
            movieCrawlingService.crawlAndSaveMovies();
            log.info("✅ 영화 정보 크롤링이 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            log.error("❌ 영화 정보 크롤링 중 오류가 발생했습니다: {}", e.getMessage());
            // 애플리케이션 시작을 중단하지 않고 로그만 남김
        }
    }
} 