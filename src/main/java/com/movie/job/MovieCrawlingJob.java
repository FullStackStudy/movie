package com.movie.job;

import com.movie.service.movie.MovieCrawlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieCrawlingJob implements Job {

    private final MovieCrawlingService movieCrawlingService;

    @Override
    public void execute(JobExecutionContext context) { // 스케쥴러 테스트 로그찍기용
        try {
            log.info("🎬 영화 크롤링 Job 시작 - {}", context.getJobDetail().getKey());
            movieCrawlingService.crawlAndSaveMovies();
            log.info("✅ 영화 크롤링 Job 완료");
        } catch (Exception e) {
            log.error("❌ 영화 크롤링 Job 실패: {}", e.getMessage(), e);
        }
    }
}