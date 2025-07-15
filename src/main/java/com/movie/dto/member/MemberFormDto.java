package com.movie.dto.member;

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

    @Email
    @NotBlank
    private String memberId;

    @NotEmpty
    @Length(min = 8)
    private String password;

    @NotBlank
    private String name;

    private LocalDate birth;

    private String phone;

    private String address;

    private String nickname;
} 