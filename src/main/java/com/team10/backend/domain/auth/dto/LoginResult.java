package com.team10.backend.domain.auth.dto;

public record LoginResult(
        LoginResponse response,
        String accessToken,
        String refreshToken
) {}
