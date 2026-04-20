package com.team10.backend.domain.feed.controller;

import com.team10.backend.domain.feed.dto.post.CreateFeedRequestDto;
import com.team10.backend.domain.feed.dto.post.CreateFeedResponseDto;
import com.team10.backend.domain.feed.dto.post.FeedLikeToggleResponseDto;
import com.team10.backend.domain.feed.dto.post.FeedListResponseDto;
import com.team10.backend.domain.feed.dto.post.UpdateFeedRequestDto;
import com.team10.backend.domain.feed.dto.post.UpdateFeedResponseDto;
import com.team10.backend.domain.feed.service.FeedPostService;
import com.team10.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "피드 관리 API")
public class FeedController {

    private final FeedPostService feedPostService;

    @GetMapping("/{sellerId}/feeds")
    @Operation(summary = "피드 조회", description = "피드 전체 조회 합니다.")
    public ApiResponse<FeedListResponseDto> getStoreFeeds(
            @PathVariable Long sellerId,
            Authentication authentication
    ) {
        FeedListResponseDto response = feedPostService.getFeedsList(sellerId, nullableCurrentUserId(authentication));
        return ApiResponse.ok(response);
    }

    @PostMapping("me/feeds")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "피드 생성", description = "판매자가 피드를 생성 합니다.")
    public ApiResponse<CreateFeedResponseDto> createFeed(
            @RequestBody @Valid CreateFeedRequestDto requestDto,
            Authentication authentication
    ) {
        CreateFeedResponseDto responseDto = feedPostService.createFeed(requestDto, currentUserId(authentication));
        return ApiResponse.ok(responseDto);
    }

    @PatchMapping("me/feeds/{feedId}")
    @Operation(summary = "피드 수정", description = "판매자가 피드를 수정합니다.")
    public ApiResponse<UpdateFeedResponseDto> updateFeed(
            @PathVariable Long feedId,
            @RequestBody @Valid UpdateFeedRequestDto requestDto,
            Authentication authentication
    ) {
        UpdateFeedResponseDto responseDto =
                feedPostService.updateFeed(feedId, requestDto, currentUserId(authentication));

        return ApiResponse.ok(responseDto);
    }

    @DeleteMapping("me/feeds/{feedId}")
    @Operation(summary = "피드 삭제", description = "판매자가 피드를 삭제합니다.")
    public ApiResponse<Void> deleteFeed(
            @PathVariable Long feedId,
            Authentication authentication
    ) {
        feedPostService.deleteFeed(feedId, currentUserId(authentication));

        return ApiResponse.ok();
    }

    @PostMapping("me/feeds/{feedId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "피드 좋아요", description = "사용자가 피드에 좋아요를 누릅니다.")
    public ApiResponse<FeedLikeToggleResponseDto> toggleFeedLike(
            @PathVariable Long feedId,
            Authentication authentication
    ) {
        FeedLikeToggleResponseDto responseDto =
                feedPostService.toggleFeedLike(feedId, currentUserId(authentication));

        return ApiResponse.ok(responseDto);
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
