package com.team10.backend.domain.user.dto;

import com.team10.backend.domain.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AuthRegisterRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$",
                message = "비밀번호는 8~20자이며, 영문과 숫자를 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9가-힣_]{2,20}$",
                message = "닉네임은 2~20자이며, 영문/숫자/한글/_만 사용할 수 있습니다."
        )
        String nickname,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(
                regexp = "^010-?\\d{4}-?\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (010-1234-5678)"
        )
        String phoneNumber,

        @NotBlank(message = "주소는 필수입니다.")
        String address,

        @NotNull(message = "역할은 필수입니다.")
        Role role
) {}
