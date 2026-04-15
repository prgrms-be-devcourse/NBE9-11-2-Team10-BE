package com.team10.backend.domain.user.dto;

import com.team10.backend.domain.user.enums.DuplicateType;

public record DuplicateCheckResponse(
        DuplicateType type,
        String value,
        boolean available
) {}
