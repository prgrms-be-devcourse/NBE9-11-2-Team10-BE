package com.team10.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerUpdateRequest(
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

        @Size(max = 500, message = "소개글은 500자 이하입니다.")
        String bio,

        String businessNumber
) {}
