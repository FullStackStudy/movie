package com.movie.init;

import com.movie.constant.Role;
import com.movie.entity.Member;
import com.movie.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestAccountInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createTestAccounts();
    }

    private void createTestAccounts() {
        // 테스트용 USER 계정 생성
        if (!memberRepository.existsById("test@test.com")) {
            Member testUser = new Member();
            testUser.setMemberId("test@test.com");
            testUser.setPassword(passwordEncoder.encode("1234"));
            testUser.setName("테스트유저");
            testUser.setBirth(LocalDate.of(1990, 1, 1));
            testUser.setPhone("010-1234-5678");
            testUser.setAddress("서울시 강남구 테스트동 123-45");
            testUser.setNickname("테스트유저");
            testUser.setRole(Role.USER);
            testUser.setRegDate(LocalDate.now());
            testUser.setGrade("일반");
            testUser.setReserve("쥬라기 월드: 새로운 시작");
            testUser.setPoint("0");
            testUser.setInquiry("");

            memberRepository.save(testUser);
            log.info("✅ 테스트용 USER 계정 생성 완료: test@test.com / 1234");
        }

        // 테스트용 ADMIN 계정 생성
        if (!memberRepository.existsById("admin@admin.com")) {
            Member testAdmin = new Member();
            testAdmin.setMemberId("admin@admin.com");
            testAdmin.setPassword(passwordEncoder.encode("1234"));
            testAdmin.setName("관리자");
            testAdmin.setBirth(LocalDate.of(1985, 5, 15));
            testAdmin.setPhone("010-9876-5432");
            testAdmin.setAddress("서울시 서초구 관리동 456-78");
            testAdmin.setNickname("관리자");
            testAdmin.setRole(Role.ADMIN);
            testAdmin.setRegDate(LocalDate.now());
            testAdmin.setGrade("관리자");
            testAdmin.setReserve("테스트 영화, 샘플 영화");
            testAdmin.setPoint("0");
            testAdmin.setInquiry("");

            memberRepository.save(testAdmin);
            log.info("✅ 테스트용 ADMIN 계정 생성 완료: admin@admin.com / 1234");
        }

        log.info("🎬 테스트 계정 정보:");
        log.info("   👤 USER 계정: test@test.com / 1234");
        log.info("   👨‍💼 ADMIN 계정: admin@admin.com / 1234");
    }
} 