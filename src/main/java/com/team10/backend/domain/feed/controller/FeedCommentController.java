package com.team10.backend.domain.feed.controller;

import com.team10.backend.domain.feed.dto.comment.CommentLikeToggleResponseDto;
import com.team10.backend.domain.feed.dto.comment.CommentListResponseDto;
import com.team10.backend.domain.feed.dto.comment.CommentResponseDto;
import com.team10.backend.domain.feed.dto.comment.CreateCommentRequestDto;
import com.team10.backend.domain.feed.dto.comment.UpdateCommentRequestDto;
import com.team10.backend.domain.feed.service.FeedCommentService;
import com.team10.backend.global.dto.ApiResponse;
import com.team10.backend.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores/{sellerId}/feeds/{feedId}/comments")
@RequiredArgsConstructor
@Tag(name = "Feed_Comment", description = "피드 댓글 관리 API")
public class FeedCommentController {

    private final FeedCommentService feedCommentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "댓글 생성", description = "피드에 댓글을 생성 합니다.")
    public ApiResponse<CommentResponseDto> createComment(
            @PathVariable Long sellerId,
            @PathVariable Long feedId,
            @RequestBody @Valid CreateCommentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserPrincipal currentUser
    ) {
        return ApiResponse.ok(feedCommentService.createComment(sellerId, feedId, requestDto, currentUser.userId()));
    }

    @GetMapping
    @Operation(summary = "댓글 조회", description = "피드의 댓글을 조회 합니다.")
    public ApiResponse<CommentListResponseDto> getComments(
            @PathVariable Long sellerId,
            @PathVariable Long feedId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal CustomUserPrincipal currentUser
    ) {
        Long currentUserId = currentUser != null ? currentUser.userId() : null;

        return ApiResponse.ok(feedCommentService.getComments(sellerId, feedId, page, size, sort, currentUserId));
    }

    @PatchMapping("/{commentId}")
    @Operation(summary = "댓글 수정", description = "피드의 댓글을 수정합니다.")
    public ApiResponse<CommentResponseDto> updateComment(
            @PathVariable Long sellerId,
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserPrincipal currentUser
            ){
        return ApiResponse.ok(feedCommentService.updateComment(sellerId, feedId, commentId, requestDto, currentUser.userId()));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "피드의 댓글을 삭제 합니다.")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long sellerId,
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser
    ) {
        feedCommentService.deleteComment(sellerId, feedId, commentId, currentUser.userId());
        return ApiResponse.ok();
    }


    @PostMapping("/{commentId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "댓글 좋아요", description = "사용자가 피드 댓글에 좋아요를 누릅니다.")
    public ApiResponse<CommentLikeToggleResponseDto> toggleCommentLike(
            @PathVariable Long sellerId,
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser
    ) {
        return ApiResponse.ok(feedCommentService.toggleCommentLike(sellerId, feedId, commentId, currentUser.userId()));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return Long.valueOf(authentication.getName());
    }

    private Long nullableCurrentUserId(Authentication authentication) {
        return currentUserId(authentication);
    }

}
