package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.order.enums.DeliveryStatus;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "order_delivery")
public class OrderDelivery extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "delivery_address")
    private String delivery_address;

    @Column(name = "tracking_number")
    private String tracking_number;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    private DeliveryStatus status;

    private OrderDelivery(String delivery_address, String tracking_number) {
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

    public Order getOrder() {
        return this.order;
    }

    public String getDelivery_address() {
        return this.delivery_address;
    }

    public String getTracking_number() {
        return this.tracking_number;
    }

    public DeliveryStatus getStatus() {
        return this.status;
    }

    protected OrderDelivery() {
    }

    public static class OrderDeliveryBuilder {
        private String delivery_address;
        private String tracking_number;

        OrderDeliveryBuilder() {
        }

        public OrderDeliveryBuilder delivery_address(String delivery_address) {
            this.delivery_address = delivery_address;
            return this;
        }

        public OrderDeliveryBuilder tracking_number(String tracking_number) {
            this.tracking_number = tracking_number;
            return this;
        }

        public OrderDelivery build() {
            return new OrderDelivery(this.delivery_address, this.tracking_number);
        }

        public String toString() {
            return "OrderDelivery.OrderDeliveryBuilder(delivery_address=" + this.delivery_address + ", tracking_number=" + this.tracking_number + ")";
        }
    }

    public static OrderDeliveryBuilder builder() {
        return new OrderDeliveryBuilder();
    }
}
