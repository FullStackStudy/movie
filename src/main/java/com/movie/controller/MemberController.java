package com.movie.controller;

import com.movie.dto.MemberFormDto;
import com.movie.entity.Member;
import com.movie.service.MemberService;
import com.movie.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;

    @GetMapping("/members/auth")
    public String authForm(Model model) {
        model.addAttribute("memberFormDto", new MemberFormDto());
        // 기본적으로 로그인 탭 표시
        model.addAttribute("showSignUp", false);
        return "member/loginForm";
    }

    @PostMapping("/members/auth")
    public String authForm(@Valid MemberFormDto memberFormDto, BindingResult bindingResult,
                          @RequestParam(required = false) String passwordConfirm, 
                          @RequestParam(required = false) String verificationCode, Model model) {
        System.out.println("📝 인증 요청 받음");
        System.out.println("📋 입력 데이터: " + memberFormDto);
        System.out.println("🔐 비밀번호 확인: " + passwordConfirm);
        System.out.println("📧 인증 코드: " + verificationCode);
        System.out.println("🔍 passwordConfirm null 체크: " + (passwordConfirm == null));
        System.out.println("🔍 passwordConfirm empty 체크: " + (passwordConfirm != null && passwordConfirm.trim().isEmpty()));
        
        // 회원가입 요청인지 확인 (passwordConfirm이 있으면 회원가입)
        if (passwordConfirm != null && !passwordConfirm.trim().isEmpty()) {
            System.out.println("🆕 회원가입 요청으로 판단");
            
            if (bindingResult.hasErrors()) {
                System.out.println("❌ 유효성 검사 오류:");
                bindingResult.getAllErrors().forEach(error -> {
                    System.out.println("   - " + error.getDefaultMessage());
                });
                model.addAttribute("showSignUp", true); // 회원가입 탭 표시
                return "member/loginForm";
            }

            // 이메일 인증 확인
            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                System.out.println("❌ 이메일 인증 코드 없음");
                model.addAttribute("errorMessage", "이메일 인증을 완료해주세요.");
                model.addAttribute("memberFormDto", memberFormDto);
                model.addAttribute("showSignUp", true);
                return "member/loginForm";
            }
            
            // 서버에서 이메일 인증 코드 검증 (회원가입용 - 이미 인증된 토큰도 확인 가능)
            try {
                System.out.println("🔍 회원가입용 이메일 인증 검증 시작");
                System.out.println("   - 이메일: " + memberFormDto.getMemberId());
                System.out.println("   - 인증 코드: " + verificationCode);
                
                boolean isVerified = emailVerificationService.verifyCodeForSignup(memberFormDto.getMemberId(), verificationCode);
                System.out.println("   - 검증 결과: " + isVerified);
                
                if (!isVerified) {
                    System.out.println("❌ 이메일 인증 코드 검증 실패");
                    model.addAttribute("errorMessage", "인증 코드가 올바르지 않거나 만료되었습니다.");
                    model.addAttribute("memberFormDto", memberFormDto);
                    model.addAttribute("showSignUp", true);
                    return "member/loginForm";
                }
                System.out.println("✅ 이메일 인증 코드 검증 성공");
            } catch (Exception e) {
                System.out.println("❌ 이메일 인증 검증 중 오류: " + e.getMessage());
                model.addAttribute("errorMessage", "이메일 인증 확인 중 오류가 발생했습니다.");
                model.addAttribute("memberFormDto", memberFormDto);
                model.addAttribute("showSignUp", true);
                return "member/loginForm";
            }

            // 비밀번호 확인 검증
            System.out.println("🔐 비밀번호 검증 시작");
            System.out.println("   - 원본 비밀번호: " + memberFormDto.getPassword());
            System.out.println("   - 확인 비밀번호: " + passwordConfirm);
            System.out.println("   - null 체크: " + (passwordConfirm == null));
            System.out.println("   - 일치 여부: " + memberFormDto.getPassword().equals(passwordConfirm));
            
            if (passwordConfirm == null || !memberFormDto.getPassword().equals(passwordConfirm)) {
                System.out.println("❌ 비밀번호 불일치");
                System.out.println("🔄 회원가입 탭으로 리다이렉트");
                model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
                model.addAttribute("memberFormDto", memberFormDto); // 폼 데이터 유지
                model.addAttribute("showSignUp", true); // 회원가입 탭 표시
                return "member/loginForm";
            }
            
            System.out.println("✅ 비밀번호 일치 확인");

            try {
                System.out.println("✅ 유효성 검사 통과, 회원 생성 시작");
                Member member = Member.createMember(memberFormDto, memberService.getPasswordEncoder());
                System.out.println("👤 생성된 회원: " + member);
                memberService.saveMember(member);
                System.out.println("✅ 회원가입 완료");
                
                // 회원가입 성공 시 알림 메시지와 함께 리다이렉트
                return "redirect:/?signupSuccess=true";
            } catch (IllegalStateException e) {
                System.out.println("❌ 회원가입 실패: " + e.getMessage());
                model.addAttribute("errorMessage", e.getMessage());
                model.addAttribute("memberFormDto", memberFormDto); // 폼 데이터 유지
                model.addAttribute("showSignUp", true); // 회원가입 탭 표시
                return "member/loginForm";
            }
        } else {
            // 로그인 요청인 경우 (Spring Security가 처리)
            System.out.println("🔑 로그인 요청으로 판단");
            System.out.println("❌ passwordConfirm이 없거나 비어있음 - 로그인으로 처리");
            return "redirect:/login";
        }
    }

    @GetMapping("/members/login/error")
    public String loginError(Model model) {
        model.addAttribute("loginErrorMsg", "아이디 또는 비밀번호를 확인해주세요");
        model.addAttribute("showSignUp", false); // 로그인 탭 유지
        return "member/loginForm";
    }

}