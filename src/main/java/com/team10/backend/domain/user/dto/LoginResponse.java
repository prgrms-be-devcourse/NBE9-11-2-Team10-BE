package com.team10.backend.domain.user.dto;

import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.Role;

public record LoginResponse(
        Long id,
        String email,
        String nickname,
        Role role
) {

    public static LoginResponse from(User user) {
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole()
        );
    }

}
