package com.movie.entity;

import com.movie.constant.Role;
import com.movie.dto.MemberFormDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Entity
@Table(name = "member")
@Getter
@Setter
@ToString
public class Member {

    @Id
    @Email
    @Column(name = "member_id",nullable = false, unique = true) // 중복 X
    private String memberId;

    @Column(name = "member_pw", nullable = false)
    private String password;

    @Column(name = "member_name", nullable = false)
    private String name;

    @Column(name = "member_birth")
    private LocalDate birth;

    @Column(name = "member_phone")
    private String phone;

    @Column(name = "member_address")
    private String address;

    @Column(name = "member_profile")
    private String profile = "default-profile.png"; // 기본값으로 초기화

    @Column(name = "member_nickname")
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false)
    private Role role;

    @Column(name = "member_regdate", nullable = false)
    private LocalDate regDate;

    @Column(name = "member_grade")
    private String grade;

    @Column(name = "member_reserve")
    private String reserve;

    @Column(name = "member_point")
    private String point;

    @Column(name = "member_inquiry")
    private String inquiry;

    // 엔티티가 저장되기 전에 null 값들을 기본값으로 설정
    @PrePersist
    public void prePersist() {
        // null인 경우에만 기본값 설정
        if (this.birth == null) {
            this.birth = LocalDate.now();
        }
        if(this.nickname == null){
            this.nickname="닉네임 미입력";
        }
        if (this.phone == null) {
            this.phone = "00000000000";
        }
        if (this.address == null) {
            this.address = "주소 미입력";
        }
        if (this.profile == null) {
            this.profile = "default-profile.png";
        }
        if (this.reserve == null) {
            this.reserve = "예약 내역이 없습니다.";
        }
        if (this.point == null) {
            this.point = "0";
        }
        if (this.inquiry == null) {
            this.inquiry = "0";
        }
    }

    public static Member createMember(MemberFormDto memberFormDto, PasswordEncoder passwordEncoder) {
        Member member = new Member();
        member.setMemberId(memberFormDto.getMemberId());
        member.setName(memberFormDto.getName());
        
        // 선택사항 필드들 - null이 아닌 경우에만 설정
        if (memberFormDto.getBirth() != null) {
            member.setBirth(memberFormDto.getBirth());
        }
        if (memberFormDto.getPhone() != null && !memberFormDto.getPhone().trim().isEmpty()) {
            member.setPhone(memberFormDto.getPhone());
        }
        if (memberFormDto.getAddress() != null && !memberFormDto.getAddress().trim().isEmpty()) {
            member.setAddress(memberFormDto.getAddress());
        }
        if (memberFormDto.getNickname() != null && !memberFormDto.getNickname().trim().isEmpty()) {
            member.setNickname(memberFormDto.getNickname());
        }
        
        String password = passwordEncoder.encode(memberFormDto.getPassword());
        member.setPassword(password);
        
        // 필수 필드 설정
        member.setRole(Role.USER); // 기본 역할
        member.setRegDate(LocalDate.now()); // 가입일
        member.setGrade("일반"); // 기본 등급
        
        // 기본값들은 @PrePersist에서 자동으로 설정됨
        return member;
    }
}
