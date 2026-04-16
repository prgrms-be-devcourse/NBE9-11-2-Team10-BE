package com.team10.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // === 공통 (1000~1999) ===
    INTERNAL_SERVER_ERROR("COMMON_001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("COMMON_002", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),

    // === 사용자 도메인 (2000~2999) ===
    USER_NOT_FOUND("USER_001", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("USER_002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME("USER_003", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),

    // === 상품 도메인 (3000~3999) ===,
    PRODUCT_NOT_FOUND("PRODUCT_001", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK("PRODUCT_002", "재고가 부족합니다.", HttpStatus.BAD_REQUEST),

    // === 피드 도메인 (4000~4999) ===
    FEED_NOT_FOUND("FEED_001", "피드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FEED_COMMENT_NOT_FOUND("FEED_COMMENT_001", "피드 댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FEED_COMMENT_ACCESS_DENIED("FEED_COMMENT_002", "피드 댓글에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;        // 프론트에서 분기용 비즈니스 코드
    private final String message;     // 기본 메시지 (상세 설명은 동적 생성 가능)
    private final HttpStatus status;  // HTTP 상태코드
}
