package com.team10.backend.domain.image.dto;

public record PresignedUrlResponse(
        String uploadUrl,
        String imageUrl
) {
}
