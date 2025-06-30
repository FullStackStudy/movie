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

    @Column(name = "member_birth", nullable = false)
    private LocalDate birth;

    @Column(name = "member_phone", nullable = false)
    private String phone;

    @Column(name = "member_address", nullable = false)
    private String address;

    @Column(name = "member_profile")
    private String profile;

    @Column(name = "member_nickname", nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false)
    private Role role;

    @Column(name = "member_regdate", nullable = false)
    private LocalDate regDate;

    @Column(name = "member_grade", nullable = false)
    private String grade;

    @Column(name = "member_reserve")
    private String reserve;

    @Column(name = "member_point")
    private String point;

    @Column(name = "member_inquiry")
    private String inquiry;

    public static Member createMember(MemberFormDto memberFormDto, PasswordEncoder passwordEncoder) {
        Member member = new Member();
        member.setMemberId(memberFormDto.getMemberId());
        member.setName(memberFormDto.getName());
        member.setBirth(memberFormDto.getBirth());
        member.setPhone(memberFormDto.getPhone());
        member.setAddress(memberFormDto.getAddress());
        member.setNickname(memberFormDto.getNickname());
        String password = passwordEncoder.encode(memberFormDto.getPassword());
        member.setPassword(password);
        member.setRole(Role.USER); // 기본 역할은 USER로 설정
        member.setRegDate(LocalDate.now()); // 가입일은 현재 날짜로 설정
        member.setGrade("일반"); // 기본 등급 설정
        return member;
    }
}
