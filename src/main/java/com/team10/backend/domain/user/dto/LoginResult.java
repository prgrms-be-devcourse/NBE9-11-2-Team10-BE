package com.team10.backend.domain.user.dto;

public record LoginResult(
        LoginResponse response,
        String accessToken
) {}
