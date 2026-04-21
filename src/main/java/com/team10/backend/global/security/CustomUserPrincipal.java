package com.team10.backend.global.security;

import com.team10.backend.domain.user.enums.Role;

public record CustomUserPrincipal(
        Long userId,
        Role role
) {}
