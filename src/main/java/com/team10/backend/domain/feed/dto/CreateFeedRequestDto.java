package com.team10.backend.domain.feed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateFeedRequestDto(
        @NotBlank(message = "피드 내용은 필수입니다.")
        @Size(min = 1, max = 2000, message = "내용은 1자 이상 2,000자 이하로 입력해주세요.")
        String content,

        @Size(max = 10, message = "미디어 파일은 최대 10개까지 업로드 가능합니다.")
        List<String> mediaUrls
) {
}