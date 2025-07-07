package com.movie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class MemberFormDto {

    @Email(message = "올바른 이메일 형식으로 입력해주세요.")
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    private String memberId;

    @NotEmpty(message = "비밀번호는 필수 입력 항목입니다.")
    @Length(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    private String name;

    private LocalDate birth;

    private String phone;

    private String address;

    private String nickname;
}