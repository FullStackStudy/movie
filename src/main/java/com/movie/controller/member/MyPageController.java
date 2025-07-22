package com.movie.controller.member;

import com.movie.dto.member.MyPageDto;
import com.movie.dto.payment.MovieOrderDto;
import com.movie.dto.store.StoreMemberPaymentInfoDto;
import com.movie.entity.member.Member;
import com.movie.repository.member.MemberRepository;
import com.movie.service.member.MemberService;
import com.movie.service.common.FileService;
import com.movie.service.payment.MovieOrderService;
import com.movie.service.store.StoreMemberPaymentInfoService;
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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final MemberRepository memberRepository;
    private final MovieOrderService movieOrderService;
    private final StoreMemberPaymentInfoService storeMemberPaymentInfoService;

    @GetMapping("/mypage")
    public String myPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String memberId = getMemberIdFromAuthentication(auth);
        
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
            
            // 주문 내역 조회
            List<MovieOrderDto> orderList = movieOrderService.getOrdersByMemberId(memberId);
            model.addAttribute("orderList", orderList);
        } catch (Exception e) {
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
    public String changePasswordForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String memberId = getMemberIdFromAuthentication(auth);
        
        try {
            Member member = memberRepository.findById(memberId).orElseThrow();
            boolean isOAuth2User = "OAUTH2_USER".equals(member.getPassword());
            model.addAttribute("isOAuth2User", isOAuth2User);
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "사용자 정보를 불러올 수 없습니다.");
        }
        
        return "mypage/changePassword";
    }

    @PostMapping("/mypage/change-password")
    public String changePassword(@RequestParam(required = false) String currentPassword, 
                                @RequestParam String newPassword, 
                                @RequestParam String confirmPassword, 
                                Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String memberId = getMemberIdFromAuthentication(auth);
        
        try {
            // 사용자 정보 조회하여 소셜 로그인 사용자 여부 확인
            Member member = memberRepository.findById(memberId).orElseThrow();
            boolean isOAuth2User = "OAUTH2_USER".equals(member.getPassword());
            model.addAttribute("isOAuth2User", isOAuth2User);
            
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
            
            // 소셜 로그인 사용자가 아닌 경우 현재 비밀번호 필수
            if (!isOAuth2User && (currentPassword == null || currentPassword.trim().isEmpty())) {
                model.addAttribute("errorMessage", "현재 비밀번호를 입력해주세요.");
                return "mypage/changePassword";
            }
            
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
            
            // 카카오 로그인의 경우 kakao_account에서 이메일 추출
            String email = null;
            Map<String, Object> attributes = oauth2User.getAttributes();
            
            // 카카오 로그인인지 확인 (id 속성이 있으면 카카오)
            if (attributes.containsKey("id")) {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null) {
                    email = (String) kakaoAccount.get("email");
                    System.out.println("🔍 카카오 계정에서 이메일 추출: " + email);
                }
            } else {
                // 다른 OAuth2 제공자 (Google 등)의 경우
                email = oauth2User.getAttribute("email");
                System.out.println("🔍 일반 OAuth2에서 이메일 추출: " + email);
            }
            
            // 이메일이 없는 경우 카카오 ID를 사용
            if (email == null || email.trim().isEmpty()) {
                String oauthId = oauth2User.getName(); // 카카오 ID
                email = oauthId + "@kakao.com";
                System.out.println("⚠️ OAuth2User에서 이메일 없음, 카카오 ID 사용: " + email);
            } else {
                System.out.println("✅ OAuth2User에서 실제 이메일 사용: " + email);
            }
            
            return email;
        }
        
        // 일반 로그인 사용자인 경우
        String memberId = auth.getName();
        return memberId;
    }
    @GetMapping("mypage/storeOrders")
    public String getStoreOrdersPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login"; // 비로그인 시 로그인 페이지로
        }

        String memberId = principal.getName(); // 회원 아이디 또는 이메일 등
        List<StoreMemberPaymentInfoDto> orders = storeMemberPaymentInfoService.getOrderHistoryByMemberId(memberId);
        model.addAttribute("orders", orders);

        return "mypage/storeOrders"; // templates/mypage/storeOrders.html
    }
} 