package com.team10.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record ProfileImageUpdateRequest(
        @NotBlank(message = "이미지 URL은 필수입니다.")
        @URL(message = "올바른 이미지 URL 형식이어야 합니다.")
        String imageUrl
) {
}
