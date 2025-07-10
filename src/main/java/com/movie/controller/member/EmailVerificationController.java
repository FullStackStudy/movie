package com.movie.controller.member;

import com.movie.dto.member.EmailVerificationConfirmRequest;
import com.movie.dto.member.EmailVerificationRequest;
import com.movie.repository.member.MemberRepository;
import com.movie.service.member.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "이메일 인증", description = "이메일 인증 관련 API")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final MemberRepository memberRepository;

    @Operation(summary = "이메일 인증 코드 전송")
    @PostMapping("/send-code")
    public ResponseEntity<Map<String, String>> sendVerificationCode(@Valid @RequestBody EmailVerificationRequest request) {
        try {
            emailVerificationService.sendVerificationCode(request.getEmail());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "인증 코드가 이메일로 전송되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("이메일 인증 코드 전송 실패: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "이메일 인증 코드 확인")
    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, String>> verifyCode(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        try {
            boolean isValid = emailVerificationService.verifyCode(request.getEmail(), request.getCode());
            
            Map<String, String> response = new HashMap<>();
            if (isValid) {
                response.put("message", "이메일 인증이 완료되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "인증 코드가 올바르지 않거나 만료되었습니다.");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("이메일 인증 코드 확인 실패: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "인증 확인 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "이메일 중복 확인")
    @PostMapping("/check-duplicate")
    public ResponseEntity<Map<String, Object>> checkDuplicateEmail(@Valid @RequestBody EmailVerificationRequest request) {
        try {
            boolean exists = memberRepository.existsById(request.getEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            response.put("message", exists ? "이미 가입된 이메일입니다." : "사용 가능한 이메일입니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("이메일 중복 확인 실패: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "중복 확인 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 