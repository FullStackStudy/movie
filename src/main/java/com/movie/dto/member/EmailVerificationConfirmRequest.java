package com.movie.dto.member;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationConfirmRequest {
    
    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    private String email;
    
    @NotBlank(message = "인증 코드는 필수 입력 값입니다.")
    private String code;
} 