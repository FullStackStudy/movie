package com.movie.controller.member;

import com.movie.dto.member.MyPageDto;
import com.movie.entity.member.Member;
import com.movie.repository.member.MemberRepository;
import com.movie.service.member.MemberService;
import com.movie.service.common.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final MemberRepository memberRepository;

    @GetMapping("/mypage")
    public String myPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String memberId = getMemberIdFromAuthentication(auth);
        
        System.out.println("🔍 마이페이지 접근 - 인증 정보: " + auth.getName());
        System.out.println("📧 추출된 이메일: " + memberId);
        
        try {
            Member member = memberRepository.findById(memberId).orElseThrow();
            System.out.println("✅ 회원 정보 조회 성공: " + member.getMemberId());
            
            String reserve = member.getReserve();
            List<String> reserveList = new ArrayList<>();
            if (reserve != null && !reserve.isEmpty()) {
                reserveList = Arrays.asList(reserve.split("\\n"));
            }
            model.addAttribute("reserveList", reserveList);
            
            MyPageDto myPageDto = memberService.getMyPageInfo(memberId);
            model.addAttribute("myPageDto", myPageDto);
        } catch (Exception e) {
            System.out.println("❌ 마이페이지 오류: " + e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
        }
        
        return "mypage/mypage";
    }

    @GetMapping("/mypage/edit")
    public String editMyPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String memberId = getMemberIdFromAuthentication(auth);
        
        try {
            MyPageDto myPageDto = memberService.getMyPageInfo(memberId);
            model.addAttribute("myPageDto", myPageDto);
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/mypage";
        }
        
        return "mypage/editMyPage";
    }

    @PostMapping("/mypage/edit")
    public String updateMyPage(@Valid MyPageDto myPageDto, 
                              BindingResult bindingResult, 
                              @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                              Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String memberId = getMemberIdFromAuthentication(auth);
        
        if (bindingResult.hasErrors()) {
            return "mypage/editMyPage";
        }
        
        try {
            // 프로필 이미지 업로드 처리
            if (profileImage != null && !profileImage.isEmpty()) {
                // 파일 크기 검증 (20MB 제한)
                if (profileImage.getSize() > 20 * 1024 * 1024) {
                    model.addAttribute("errorMessage", "파일 크기는 20MB 이하여야 합니다.");
                    return "mypage/editMyPage";
                }
                
                // 파일 타입 검증
                String contentType = profileImage.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    model.addAttribute("errorMessage", "이미지 파일만 업로드 가능합니다.");
                    return "mypage/editMyPage";
                }
                
                // 기존 프로필 이미지 삭제
                MyPageDto currentInfo = memberService.getMyPageInfo(memberId);
                if (currentInfo.getProfile() != null) {
                    fileService.deleteFile(currentInfo.getProfile());
                }
                
                // 새 이미지 업로드
                String uploadedFilePath = fileService.uploadFile(profileImage);
                myPageDto.setProfile(uploadedFilePath);
            }
            
            memberService.updateMemberInfo(memberId, myPageDto);
            model.addAttribute("message", "회원정보가 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "회원정보 수정 중 오류가 발생했습니다: " + e.getMessage());
            return "mypage/editMyPage";
        }
        
        return "redirect:/mypage";
    }

    @GetMapping("/mypage/change-password")
    public String changePasswordForm() {
        return "mypage/changePassword";
    }

    @PostMapping("/mypage/change-password")
    public String changePassword(@RequestParam String currentPassword, 
                                @RequestParam String newPassword, 
                                @RequestParam String confirmPassword, 
                                Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String memberId = getMemberIdFromAuthentication(auth);
        
        // 새 비밀번호 확인
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "새 비밀번호가 일치하지 않습니다.");
            return "mypage/changePassword";
        }
        
        // 비밀번호 길이 검사
        if (newPassword.length() < 8 || newPassword.length() > 16) {
            model.addAttribute("errorMessage", "비밀번호는 8자 이상, 16자 이하로 입력해주세요.");
            return "mypage/changePassword";
        }
        
        try {
            memberService.updatePassword(memberId, currentPassword, newPassword);
            model.addAttribute("message", "비밀번호가 성공적으로 변경되었습니다.");
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "mypage/changePassword";
        }
        
        return "redirect:/mypage";
    }
    
    /**
     * 인증 정보에서 회원 ID(이메일)를 추출하는 메서드
     * 소셜 로그인 사용자의 경우 OAuth2User의 attributes에서 이메일을 가져옴
     */
    private String getMemberIdFromAuthentication(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        // 소셜 로그인 사용자인 경우 (OAuth2User)
        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                (org.springframework.security.oauth2.core.user.OAuth2User) auth.getPrincipal();
            
            String email = oauth2User.getAttribute("email");
            System.out.println("🔐 OAuth2 사용자 이메일 추출: " + email);
            return email;
        }
        
        // 일반 로그인 사용자인 경우
        String memberId = auth.getName();
        System.out.println("🔐 일반 사용자 ID: " + memberId);
        return memberId;
    }
} 