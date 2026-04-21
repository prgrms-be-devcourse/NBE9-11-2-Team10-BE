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
    ACCESS_DENIED("COMMON_003", "해당 리소스에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    FILE_UPLOAD_FAILED("COMMON_004", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_IMAGE_FILE("COMMON_005", "이미지 파일만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
    FILE_DELETE_FAILED("COMMON_006", "파일 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // === 사용자 도메인 (2000~2999) ===
    USER_NOT_FOUND("USER_001", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("USER_002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME("USER_003", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    LOGIN_FAILED("USER_004", "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.NOT_FOUND),
    NOT_SELLER("USER_005", "판매자가 아닙니다.", HttpStatus.FORBIDDEN),

    // === 상품 도메인 (3000~3999) ===,
    PRODUCT_NOT_FOUND("PRODUCT_001", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK("PRODUCT_002", "재고가 부족합니다.", HttpStatus.BAD_REQUEST),
    PRODUCT_ALREADY_INACTIVE("PRODUCT_003", "이미 비활성화된 상품입니다.", HttpStatus.CONFLICT),
    INVALID_STOCK("PRODUCT_004", "재고는 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_STOCK_QUANTITY("PRODUCT_005", "증감 수량은 0 이하일 수 없습니다.", HttpStatus.BAD_REQUEST),

    // === 피드 도메인 (4000~4999) ===
    FEED_NOT_FOUND("FEED_001", "피드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("FEED_COMMENT_001", "피드 댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_ACCESS_DENIED("FEED_COMMENT_002", "피드 댓글에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // === 주문 도메인 (5000~5999) ===
    ORDER_NOT_FOUND("ORDER_001", "주문 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // === 인증 (6000-6999) ===
    MISSING_REFRESH_TOKEN("AUTH_001", "refreshToken 값이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("AUTH_002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),

    //=== 결제 도메인(6000~6999) ===
    PAYMENT_NOT_FOUND("PAYMENT_001","결제 정보가 존재하지 않습니다.",HttpStatus.NOT_FOUND),
    AMOUNT_MISMATCH("PAYMENT_002","결제 금액이 일치하지 않습니다. 위변조 위험이 있습니다.", HttpStatus.BAD_REQUEST),

    //=== 배송 도메인(7000~7999) ===
    CANNOT_CANCEL_SHIPPING_ORDER("DELIVERY_01","이미 출고된 상품은 취소할 수 없습니다.",HttpStatus.BAD_REQUEST),
    DELIVERY_NOT_FOUND("DELIVERY_02","배송 정보를 찾을 수 없습니다.",HttpStatus.NOT_FOUND),

    //====토스 외부 API 예외처리=====
    // -------승인 비즈니스 오류(400,404,403)-----
    //404
    NOT_FOUND_PAYMENT("TOSS_01","존재하지 않는 결제 정보 입니다.",HttpStatus.NOT_FOUND),
    NOT_FOUND_PAYMENT_SESSION("TOSS_02","결제 시간이 만료되어 결제 진행 데이터가 존재하지 않습니다.",HttpStatus.NOT_FOUND),

    //403
    REJECT_ACCOUNT_PAYMENT("TOSS_03","잔액부족으로 결제에 실패했습니다.",HttpStatus.FORBIDDEN),
    REJECT_CARD_PAYMENT("TOSS_04","한도초과 혹은 잔액부족으로 결제에 실패했습니다.",HttpStatus.FORBIDDEN),
    REJECT_CARD_COMPANY("TOSS_05","결제 승인이 거절되었습니다.",HttpStatus.FORBIDDEN),
    FORBIDDEN_REQUEST("TOSS_06", "허용되지 않은 요청입니다.",HttpStatus.FORBIDDEN),
    INVALID_PASSWORD("TOSS_07", "결제 비밀번호가 일치하지 않습니다.",HttpStatus.FORBIDDEN),

    //400
    ALREADY_PROCESSED_PAYMENT("TOSS_08", "이미 처리된 결제 입니다.",HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("TOSS_09",  "잘못된 요청입니다.",HttpStatus.BAD_REQUEST),
    INVALID_API_KEY("TOSS_10",   "잘못된 시크릿키 연동 정보 입니다.",HttpStatus.BAD_REQUEST),
    INVALID_REJECT_CARD("TOSS_11",   "카드 사용이 거절되었습니다. 카드사 문의가 필요합니다.",HttpStatus.BAD_REQUEST),
    INVALID_CARD_EXPIRATION("TOSS_12",   "카드 정보를 다시 확인해주세요. (유효기간)",HttpStatus.BAD_REQUEST),
    INVALID_STOPPED_CARD("TOSS_13",   "정지된 카드 입니다.",HttpStatus.BAD_REQUEST),
    INVALID_CARD_LOST_OR_STOLEN("TOSS_14",   "분실 혹은 도난 카드입니다.",HttpStatus.BAD_REQUEST),
    INVALID_CARD_NUMBER("TOSS_15",   "카드번호를 다시 확인해주세요.",HttpStatus.BAD_REQUEST),
    INVALID_ACCOUNT_INFO_RE_REGISTER("TOSS_16",    "유효하지 않은 계좌입니다. 계좌 재등록 후 시도해주세요.",HttpStatus.BAD_REQUEST),
    UNAPPROVED_ORDER_ID("TOSS_17",    "아직 승인되지 않은 주문번호입니다.",HttpStatus.BAD_REQUEST),
    //----승인 시스템 오류(토스측 오류, 500,401..)-----
    FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING("TOSS_18","결제가 완료되지 않았어요. 다시 시도해주세요.",HttpStatus.INTERNAL_SERVER_ERROR),
    UNKNOWN_PAYMENT_ERROR("TOSS_19", "내부 시스템 처리 작업이 실패했습니다. 잠시 후 다시 시도해주세요",HttpStatus.INTERNAL_SERVER_ERROR),
    FAILED_INTERNAL_SYSTEM_PROCESSING("TOSS_20", "결제에 실패했어요. 같은 문제가 반복된다면 은행이나 카드사로 문의해주세요.",HttpStatus.INTERNAL_SERVER_ERROR),
    //-----네트워크 오류 ------
    //503 서버가 현재 요청을 처리할 수 없음 (일시적 부하 또는 통신 장애).
    NETWORK_ERROR_FINAL_FAILED("NETWORK_01","결제 결과를 확인할 수 없습니다. 중복 결제를 방지하기 위해 잠시 후 결제 내역을 확인해 주세요.",HttpStatus.SERVICE_UNAVAILABLE);


    private final String code;        // 프론트에서 분기용 비즈니스 코드
    private final String message;     // 기본 메시지 (상세 설명은 동적 생성 가능)
    private final HttpStatus status;  // HTTP 상태코드
}
