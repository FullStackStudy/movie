package com.movie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class MyPageDto {

    @Email(message = "이메일 형식으로 입력해주세요.")
    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    private String memberId;

    @NotBlank(message = "닉네임은 필수 입력 값입니다.")
    private String nickname;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @NotNull(message = "생년월일은 필수 입력 값입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;

    @NotBlank(message = "휴대폰 번호는 필수 입력 값입니다.")
    private String phone;

    @NotBlank(message = "주소는 필수 입력 값입니다.")
    private String address;

    private String profile; // 프로필 UUID
    private String grade; // 등급
    private String reserve; // 예매 내역
    private String point; // 포인트
    private String inquiry; // 문의내역
    private LocalDate regDate; // 가입일
} 