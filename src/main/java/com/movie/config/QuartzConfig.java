package com.movie.config;

import com.movie.job.EmailTokenCleanupJob;
import com.movie.job.MovieCrawlingJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail movieCrawlingJobDetail() {
        return JobBuilder.newJob(MovieCrawlingJob.class)
                .withIdentity("movieCrawlingJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger movieCrawlingJobTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(movieCrawlingJobDetail())
                .withIdentity("movieCrawlingTrigger")
                // cron 표현식 (" 0초 0분 0시 *(매일) *(매월) ?(무시요일))
                // ex) "0 0 6 * * ?" : 매일 매월 06시 00분 00초 무시요일 없음
                // "0 0 */2 * * ?" : 2시간마다 실행
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 */2 * * ?"))
                .build();
    }

    @Bean
    public JobDetail emailTokenCleanupJobDetail() {
        return JobBuilder.newJob(EmailTokenCleanupJob.class)
                .withIdentity("emailTokenCleanupJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger emailTokenCleanupJobTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(emailTokenCleanupJobDetail())
                .withIdentity("emailTokenCleanupTrigger")
                // 매시간 실행
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))
                .build();
    }
}