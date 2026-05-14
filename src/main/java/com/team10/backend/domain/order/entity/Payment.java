package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.order.enums.PaymentStatus;
import com.team10.backend.domain.order.enums.RequestType;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;


@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {
    //orderNumber하고 멱등키를 2개로 하는 unique제약이 필요.
    //FAILED일때는 괜찮은데 같은 number와 멱등키를 사용해야 하는 uncertain의 경우는 어떡하지?
    //만약 내역을 남기지 않는다면 그냥 
    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber; // 토스 전송용 고유 번호 (ORD-2026...)

    @Column(name = "total_amount", nullable = false)
    private int totalAmount; // 결제 예정 금액

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status; // READY, PAID, FAILED

    @Column(name = "payment_key")
    private String paymentKey; // 결제 승인 후 토스에서 받는 키 (초기엔 null)

    @Column(name = "idempotency_key", nullable = true)//임시로 null허용
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private RequestType type;

    @Column(columnDefinition = "TEXT")
    private String responseBody; // 성공/실패 시 응답 JSON 저장

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private Payment(Order order, String orderNumber, int totalAmount, PaymentStatus status,
                    String idempotencyKey, RequestType type) {
        this.order = order;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : PaymentStatus.READY;
        this.idempotencyKey = idempotencyKey;
        this.type = type != null ? type : RequestType.PAYMENT;
    }

    public static Payment createPayment(Order order, String orderNumber, int amount, String idempotencyKey, RequestType type) {
        return Payment.builder()
                .order(order)
                .orderNumber(orderNumber)
                .totalAmount(amount)
                .idempotencyKey(idempotencyKey)
                .status(PaymentStatus.READY)
                .type(type)
                .build();
    }

    // 결제 승인 완료 시 호출할 비즈니스 메서드
    public void completePayment(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.PAID;
    }

    public void complete(String responseBody) {
        this.status = PaymentStatus.PAID;
        this.responseBody = responseBody;
    }

    // 결제 실패 시 호출
    public void failPayment() {
        this.status = PaymentStatus.FAILED;
    }

    public void markAsPending() {
        this.status = PaymentStatus.PENDING;
    }

    public void markAsUncertain() {
        this.status = PaymentStatus.UNCERTAIN;
    }

    public String getLastTossKey() {
        return this.idempotencyKey;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public int getTotalAmount() {
        return this.totalAmount;
    }

    public PaymentStatus getStatus() {
        return this.status;
    }

    public String getPaymentKey() {
        return this.paymentKey;
    }

    public String getIdempotencyKey() {
        return this.idempotencyKey;
    }

    public RequestType getType() {
        return this.type;
    }

    public String getResponseBody() {
        return this.responseBody;
    }

    public Order getOrder() {
        return this.order;
    }

    protected Payment() {
    }

    public static class PaymentBuilder {
        private Order order;
        private String orderNumber;
        private int totalAmount;
        private PaymentStatus status;
        private String idempotencyKey;
        private RequestType type;

        PaymentBuilder() {
        }

        public PaymentBuilder order(Order order) {
            this.order = order;
            return this;
        }

        public PaymentBuilder orderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public PaymentBuilder totalAmount(int totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public PaymentBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public PaymentBuilder type(RequestType type) {
            this.type = type;
            return this;
        }

        public Payment build() {
            return new Payment(this.order, this.orderNumber, this.totalAmount, this.status, this.idempotencyKey, this.type);
        }

        public String toString() {
            return "Payment.PaymentBuilder(order=" + this.order + ", orderNumber=" + this.orderNumber + ", totalAmount=" + this.totalAmount + ", status=" + this.status + ", idempotencyKey=" + this.idempotencyKey + ", type=" + this.type + ")";
        }
    }

    public static PaymentBuilder builder() {
        return new PaymentBuilder();
    }
}
