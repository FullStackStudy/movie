package com.movie.controller;

import com.movie.entity.Member;
import com.movie.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final MemberRepository memberRepository;

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
} 