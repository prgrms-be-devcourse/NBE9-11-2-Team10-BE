package com.team10.backend.domain.user.dto;

public record SellerUpdateRequest(
    String nickname,
    String phoneNumber,
    String address,
    String bio,
    String businessNumber
) {}
