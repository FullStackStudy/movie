package com.movie.job;

import com.movie.service.MovieCrawlingService;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieCrawlingJob implements Job {

    private final MovieCrawlingService movieCrawlingService;

    @Override
    public void execute(JobExecutionContext context) {
        movieCrawlingService.crawlAndSaveMovies();
    }
}