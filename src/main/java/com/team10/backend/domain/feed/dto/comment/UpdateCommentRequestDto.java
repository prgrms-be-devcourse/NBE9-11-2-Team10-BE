package com.team10.backend.domain.feed.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequestDto(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(min = 1, max = 500, message = "댓글은 1자 이상 500자 이하로 입력해주세요.")
        String content
) {
}
