package com.movie.service.member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[MovieFlex] 이메일 인증 코드");
            message.setText("안녕하세요! MovieFlex 회원가입을 위한 인증 코드입니다.\n\n" +
                    "인증 코드: " + verificationCode + "\n\n" +
                    "이 코드는 5분간 유효합니다.\n" +
                    "본인이 요청하지 않은 경우 이 메일을 무시하세요.\n\n" +
                    "감사합니다.\nMovieFlex 팀");

            mailSender.send(message);
            log.info("인증 이메일 전송 완료: {}", to);
        } catch (Exception e) {
            log.error("인증 이메일 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }
} 