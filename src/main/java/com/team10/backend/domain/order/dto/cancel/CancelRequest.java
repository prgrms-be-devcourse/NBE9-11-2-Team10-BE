package com.team10.backend.domain.order.dto.cancel;

public record CancelRequest(
        String cancelReason          // 취소 사유 (필수)
//        Long cancelAmount            // 취소할 금액 (부분 취소 시 필수, 누락 시 전액 취소)
) {}
