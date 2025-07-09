package com.movie.job;

import com.movie.service.member.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailTokenCleanupJob implements Job {

    private final EmailVerificationService emailVerificationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("만료된 이메일 인증 토큰 정리 시작");
            emailVerificationService.cleanupExpiredTokens();
            log.info("만료된 이메일 인증 토큰 정리 완료");
        } catch (Exception e) {
            log.error("이메일 인증 토큰 정리 중 오류 발생: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
} 