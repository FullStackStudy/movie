package com.movie.controller;

import com.movie.dto.MyPageDto;
import com.movie.entity.Member;
import com.movie.repository.MemberRepository;
import com.movie.service.MemberService;
import com.movie.service.FileService;
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
        String memberId = auth.getName();
        
        try {
            Member member = memberRepository.findById(memberId).orElseThrow();
            String reserve = member.getReserve();
            List<String> reserveList = new ArrayList<>();
            if (reserve != null && !reserve.isEmpty()) {
                reserveList = Arrays.asList(reserve.split("\\n"));
            }
            model.addAttribute("reserveList", reserveList);
            
            MyPageDto myPageDto = memberService.getMyPageInfo(memberId);
            model.addAttribute("myPageDto", myPageDto);
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        
        return "mypage/mypage";
    }

    @GetMapping("/mypage/edit")
    public String editMyPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String memberId = auth.getName();
        
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
        String memberId = auth.getName();
        
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
        String memberId = auth.getName();
        
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
} 