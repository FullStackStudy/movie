package com.movie.service;

import com.movie.entity.Member;
import com.movie.dto.MyPageDto;
import com.movie.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Member saveMember(Member member) {
        validateDuplicateMember(member);
        return memberRepository.save(member);
    }

    public Member getMemberById(String memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
    }

    public MyPageDto getMyPageInfo(String memberId) {
        Member member = getMemberById(memberId);
        MyPageDto myPageDto = new MyPageDto();
        
        myPageDto.setMemberId(member.getMemberId());
        myPageDto.setNickname(member.getNickname());
        myPageDto.setName(member.getName());
        myPageDto.setBirth(member.getBirth());
        myPageDto.setPhone(member.getPhone());
        myPageDto.setAddress(member.getAddress());
        myPageDto.setProfile(member.getProfile());
        myPageDto.setGrade(member.getGrade());
        myPageDto.setReserve(member.getReserve());
        myPageDto.setPoint(member.getPoint());
        myPageDto.setInquiry(member.getInquiry());
        myPageDto.setRegDate(member.getRegDate());
        
        return myPageDto;
    }

    public void updateMemberInfo(String memberId, MyPageDto myPageDto) {
        Member member = getMemberById(memberId);
        
        // 닉네임 중복 검사 (자신의 닉네임은 제외)
        if (!member.getNickname().equals(myPageDto.getNickname())) {
            memberRepository.findByNickname(myPageDto.getNickname()).ifPresent(m -> {
                throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
            });
        }
        
        member.setNickname(myPageDto.getNickname());
        member.setName(myPageDto.getName());
        member.setBirth(myPageDto.getBirth());
        member.setPhone(myPageDto.getPhone());
        member.setAddress(myPageDto.getAddress());
        member.setProfile(myPageDto.getProfile());
        
        memberRepository.save(member);
    }

    public void updatePassword(String memberId, String currentPassword, String newPassword) {
        Member member = getMemberById(memberId);
        
        // 현재 비밀번호 확인 (암호화된 비밀번호와 비교)
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 새 비밀번호 유효성 검사
        if (newPassword.length() < 8 || newPassword.length() > 16) {
            throw new IllegalArgumentException("비밀번호는 8자 이상, 16자 이하로 입력해주세요.");
        }
        
        // 새 비밀번호 암호화하여 저장
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        member.setPassword(encodedNewPassword);
        memberRepository.save(member);
    }

    private void validateDuplicateMember(Member member) {
        memberRepository.findById(member.getMemberId()).ifPresent(m -> {
            throw new IllegalStateException("이미 가입된 아이디입니다.");
        });
    }

    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 사용자를 찾을 수 없습니다. : " + memberId));

        return User.builder()
                .username(member.getMemberId())
                .password(member.getPassword())
                .roles(member.getRole().toString())
                .build();
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}