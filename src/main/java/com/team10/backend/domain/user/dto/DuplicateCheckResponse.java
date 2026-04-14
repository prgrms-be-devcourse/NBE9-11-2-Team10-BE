package com.team10.backend.domain.user.dto;

import com.team10.backend.domain.user.enums.DuplicateType;
import jakarta.validation.constraints.NotBlank;

public record DuplicateCheckResponse(
        @NotBlank
        DuplicateType type,
        @NotBlank
        String value,
        boolean available
) {}
