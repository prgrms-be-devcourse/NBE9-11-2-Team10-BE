package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.order.enums.DeliveryStatus;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "order_delivery")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderDelivery extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name="delivery_address")
    private String delivery_address;

    @Column(name = "tracking_number")
    private String tracking_number;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    private DeliveryStatus status;

    @Builder
    private OrderDelivery(String delivery_address,String tracking_number) {
        this.delivery_address = delivery_address;
        this.tracking_number = tracking_number;
    }

    // 결제 완료 시 호출될 메서드
    public void startReady() {
        this.status = DeliveryStatus.READY;
    }

    // 송장 입력 시 호출될 메서드
    public void updateTracking(String trackingNumber) {
        this.tracking_number = trackingNumber;
        this.status = DeliveryStatus.SHIPPING;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

}
