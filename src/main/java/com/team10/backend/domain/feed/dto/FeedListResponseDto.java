package com.team10.backend.domain.feed.dto;

import java.util.List;

public record FeedListResponseDto(
        List<FeedDto> feeds //
) {
    public static FeedListResponseDto from(List<FeedDto> feeds) {
        return new FeedListResponseDto(feeds);
    }
}