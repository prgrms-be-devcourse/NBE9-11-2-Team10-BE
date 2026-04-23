package com.team10.backend.domain.auth.dto;

import com.team10.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record AuthRegisterResponse(
        Long id,
        String email,
        LocalDateTime createdAt
) {

    public static AuthRegisterResponse from(User user) {
        return new AuthRegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }

}
