package com.movie.service;

import com.movie.entity.EmailVerificationToken;
import com.movie.repository.EmailVerificationTokenRepository;
import com.movie.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final MemberRepository memberRepository;

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 5;

    @Transactional
    public void sendVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        if (memberRepository.existsById(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 기존 토큰이 있다면 삭제
        tokenRepository.deleteByEmail(email);

        // 새로운 인증 코드 생성
        String verificationCode = generateVerificationCode();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);

        // 토큰 저장
        EmailVerificationToken token = new EmailVerificationToken(verificationCode, email, expiryDate);
        tokenRepository.save(token);

        // 이메일 전송
        emailService.sendVerificationEmail(email, verificationCode);
    }

    @Transactional
    public boolean verifyCode(String email, String code) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByEmailAndUsedFalse(email);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }

        EmailVerificationToken token = tokenOpt.get();

        // 만료 확인
        if (token.isExpired()) {
            tokenRepository.delete(token);
            return false;
        }

        // 코드 확인
        if (!token.getToken().equals(code)) {
            return false;
        }

        // 사용 완료 처리
        token.setUsed(true);
        tokenRepository.save(token);

        return true;
    }

    private String generateVerificationCode() {
        // 6자리 숫자 코드 생성
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    @Transactional
    public void cleanupExpiredTokens() {
        // 만료된 토큰들 삭제 (스케줄러에서 호출)
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.findAll().stream()
                .filter(token -> token.isExpired())
                .forEach(tokenRepository::delete);
    }
} 