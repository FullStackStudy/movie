package com.movie.controller.common;

import com.movie.entity.member.Member;
import com.movie.repository.member.MemberRepository;
import com.movie.service.movie.MovieCrawlingService;
import com.movie.service.member.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final MemberRepository memberRepository;
    private final MovieCrawlingService movieCrawlingService;
    private final EmailService emailService;

    @GetMapping("/accounts")
    public Map<String, Object> getTestAccounts() {
        Map<String, Object> result = new HashMap<>();
        
        // 테스트 계정 정보
        Map<String, Object> accounts = new HashMap<>();
        
        // USER 계정 정보
        Map<String, String> userAccount = new HashMap<>();
        userAccount.put("email", "test@test.com");
        userAccount.put("password", "1234");
        userAccount.put("role", "USER");
        userAccount.put("name", "테스트유저");
        accounts.put("user", userAccount);
        
        // ADMIN 계정 정보
        Map<String, String> adminAccount = new HashMap<>();
        adminAccount.put("email", "admin@admin.com");
        adminAccount.put("password", "1234");
        adminAccount.put("role", "ADMIN");
        adminAccount.put("name", "관리자");
        accounts.put("admin", adminAccount);
        
        result.put("testAccounts", accounts);
        result.put("message", "테스트 계정 정보입니다. 애플리케이션 시작 시 자동으로 생성됩니다.");
        
        return result;
    }

    @GetMapping("/accounts/status")
    public Map<String, Object> getTestAccountsStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // USER 계정 존재 여부 확인
        boolean userExists = memberRepository.existsById("test@test.com");
        Member user = userExists ? memberRepository.findById("test@test.com").orElse(null) : null;
        
        // ADMIN 계정 존재 여부 확인
        boolean adminExists = memberRepository.existsById("admin@admin.com");
        Member admin = adminExists ? memberRepository.findById("admin@admin.com").orElse(null) : null;
        
        Map<String, Object> status = new HashMap<>();
        
        // USER 계정 상태
        Map<String, Object> userStatus = new HashMap<>();
        userStatus.put("exists", userExists);
        if (userExists && user != null) {
            userStatus.put("name", user.getName());
            userStatus.put("nickname", user.getNickname());
            userStatus.put("role", user.getRole());
            userStatus.put("regDate", user.getRegDate());
        }
        status.put("user", userStatus);
        
        // ADMIN 계정 상태
        Map<String, Object> adminStatus = new HashMap<>();
        adminStatus.put("exists", adminExists);
        if (adminExists && admin != null) {
            adminStatus.put("name", admin.getName());
            adminStatus.put("nickname", admin.getNickname());
            adminStatus.put("role", admin.getRole());
            adminStatus.put("regDate", admin.getRegDate());
        }
        status.put("admin", adminStatus);
        
        result.put("status", status);
        result.put("message", "테스트 계정 상태 확인 완료");
        
        return result;
    }

    @GetMapping("/crawl")
    public String testCrawl() {
        try {
            log.info("테스트 크롤링 시작");
            movieCrawlingService.crawlAndSaveMovies();
            log.info("테스트 크롤링 완료");
            return "크롤링 완료!";
        } catch (Exception e) {
            log.error("테스트 크롤링 실패: {}", e.getMessage(), e);
            return "크롤링 실패: " + e.getMessage();
        }
    }

    @GetMapping("/test/email")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("메일 테스트 시작 - 수신자: {}", email);
            emailService.sendVerificationEmail(email, "TEST123");
            
            response.put("success", true);
            response.put("message", "테스트 이메일이 성공적으로 전송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("메일 테스트 실패: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "이메일 전송 실패: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getMessage());
            }
            
            return ResponseEntity.badRequest().body(response);
        }
    }
} 