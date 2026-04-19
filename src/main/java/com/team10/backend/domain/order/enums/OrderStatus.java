package com.team10.backend.domain.order.enums;

//결제 상태와 또 다르게 추가한 이유가
//만약 결제되지 않은 상황에서 취소될 경우가 있어야 한다. 그리고 이때 결제 상태도 CANCEL을 가진다.
//결제된 상황에서 취소할 경우에는 결제 상태가 REFUND가 된다. 돈을 지불하고 환불하는 경우니까.
public enum OrderStatus {
    PENDING,   // 주문 대기
    SUCCESS,      // 주문 성공
    CANCELED;  // 주문 취소
}
