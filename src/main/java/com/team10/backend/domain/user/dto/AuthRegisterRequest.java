package com.team10.backend.domain.user.dto;

import com.team10.backend.domain.user.enums.Role;

public record AuthRegisterRequest(
        String imageUrl,
        String email,
        String password,
        String name,
        String nickname,
        String phoneNumber,
        String address,
        Role role
) {}
