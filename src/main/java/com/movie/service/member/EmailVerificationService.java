package com.movie.service.member;

import com.movie.entity.member.EmailVerificationToken;
import com.movie.repository.member.EmailVerificationTokenRepository;
import com.movie.repository.member.MemberRepository;
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
        // 이미 가입된 이메일인지 확인 (회원가입용)
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
        log.info("🔍 이메일 인증 검증 시작: email={}, code={}", email, code);

        // 실제 토큰 검증
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByEmailAndUsedFalse(email);
        
        if (tokenOpt.isEmpty()) {
            log.warn("❌ 토큰을 찾을 수 없음: email={}", email);
            return false;
        }

        EmailVerificationToken token = tokenOpt.get();
        log.info("📋 토큰 정보: token={}, expiryDate={}, used={}", token.getToken(), token.getExpiryDate(), token.isUsed());

        // 만료 확인
        if (token.isExpired()) {
            log.warn("❌ 토큰 만료: email={}", email);
            tokenRepository.delete(token);
            return false;
        }

        // 코드 확인
        if (!token.getToken().equals(code)) {
            log.warn("❌ 코드 불일치: expected={}, actual={}", token.getToken(), code);
            return false;
        }

        // 사용 완료 처리
        token.setUsed(true);
        tokenRepository.save(token);
        log.info("✅ 인증 성공: email={}", email);

        return true;
    }

    /**
     * 회원가입 시 사용하는 메서드 - 이미 인증된 토큰도 확인 가능
     */
    @Transactional
    public boolean verifyCodeForSignup(String email, String code) {
        log.info("🔍 회원가입용 이메일 인증 검증 시작: email={}, code={}", email, code);
        
        // 사용된 토큰도 포함하여 검색
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByEmail(email);
        
        if (tokenOpt.isEmpty()) {
            log.warn("❌ 토큰을 찾을 수 없음: email={}", email);
            return false;
        }

        EmailVerificationToken token = tokenOpt.get();
        log.info("📋 토큰 정보: token={}, expiryDate={}, used={}", token.getToken(), token.getExpiryDate(), token.isUsed());

        // 만료 확인
        if (token.isExpired()) {
            log.warn("❌ 토큰 만료: email={}", email);
            tokenRepository.delete(token);
            return false;
        }

        // 코드 확인
        if (!token.getToken().equals(code)) {
            log.warn("❌ 코드 불일치: expected={}, actual={}", token.getToken(), code);
            return false;
        }

        // 이미 사용된 토큰이면 성공으로 처리 (회원가입 시에는 이미 인증된 상태)
        if (token.isUsed()) {
            log.info("✅ 이미 인증된 토큰 확인: email={}", email);
            return true;
        }

        // 사용 완료 처리
        token.setUsed(true);
        tokenRepository.save(token);
        log.info("✅ 인증 성공: email={}", email);

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