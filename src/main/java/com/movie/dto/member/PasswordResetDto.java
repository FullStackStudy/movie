package com.movie.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetDto {

    @Email(message = "올바른 이메일 형식으로 입력해주세요.")
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    private String email;

    @NotBlank(message = "인증 코드는 필수 입력 항목입니다.")
    private String verificationCode;

    private String newPassword;
} 