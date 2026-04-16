package com.team10.backend.domain.feed.controller;

import com.team10.backend.domain.feed.dto.CreateFeedRequestDto;
import com.team10.backend.domain.feed.dto.CreateFeedResponseDto;
import com.team10.backend.domain.feed.dto.FeedLikeToggleResponseDto;
import com.team10.backend.domain.feed.dto.FeedListResponseDto;
import com.team10.backend.domain.feed.service.FeedPostService;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.dto.ApiResponse;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final UserRepository userRepository;

    @GetMapping("/{sellerId}/feeds")
    @Operation(summary = "피드 조회", description = "피드 전체 조회 합니다.")
    public ApiResponse<FeedListResponseDto> getStoreFeeds(
            @PathVariable Long sellerId,
            @AuthenticationPrincipal User currentUser
    ) {
        FeedListResponseDto response = feedPostService.getFeedsList(sellerId, currentUser);
        return ApiResponse.ok(response);
    }

    @PostMapping("me/feeds")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "피드 생성", description = "판매자가 피드를 생성 합니다.")
    public ApiResponse<CreateFeedResponseDto> createFeed(
            @RequestBody CreateFeedRequestDto requestDto
            //@AuthenticationPrincipal User seller 로그인 후 확인
    ) {
        User seller = userRepository.findById(1L)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CreateFeedResponseDto responseDto = feedPostService.createFeed(requestDto, seller);
        return ApiResponse.ok(responseDto);
    }

    @PostMapping("{sellerId}/feeds/{feedId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "피드 좋아요", description = "사용자가 피드에 좋아요를 누릅니다.")
    public ApiResponse<FeedLikeToggleResponseDto> toggleFeedLike(
            @PathVariable Long sellerId,
            @PathVariable Long feedId
            //@AuthenticationPrincipal User buyer 로그인 후 확인
    ) {
        User currentUser = userRepository.findById(2L)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FeedLikeToggleResponseDto responseDto =
                feedPostService.toggleFeedLike(sellerId, feedId, currentUser);

        return ApiResponse.ok(responseDto);
    }
}
