package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "order_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderProducts extends BaseEntity {

    // Order와의 연관관계를 맺어주는 핵심 메서드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    private int orderPrice; // 주문 당시의 가격

    @Builder
    private OrderProducts(Product product, int quantity, int orderPrice) {
        this.product = product;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }

    // Order와의 연관관계를 맺어주는 핵심 메서드
    public void setOrder(Order order) {
        this.order = order;
    }
}
