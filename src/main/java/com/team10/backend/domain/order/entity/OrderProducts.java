package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "order_products")
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

    private OrderProducts(Product product, int quantity, int orderPrice) {
        this.product = product;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }

    // Order와의 연관관계를 맺어주는 핵심 메서드
    public void setOrder(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return this.order;
    }

    public Product getProduct() {
        return this.product;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public int getOrderPrice() {
        return this.orderPrice;
    }

    protected OrderProducts() {
    }

    public static class OrderProductsBuilder {
        private Product product;
        private int quantity;
        private int orderPrice;

        OrderProductsBuilder() {
        }

        public OrderProductsBuilder product(Product product) {
            this.product = product;
            return this;
        }

        public OrderProductsBuilder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public OrderProductsBuilder orderPrice(int orderPrice) {
            this.orderPrice = orderPrice;
            return this;
        }

        public OrderProducts build() {
            return new OrderProducts(this.product, this.quantity, this.orderPrice);
        }

        public String toString() {
            return "OrderProducts.OrderProductsBuilder(product=" + this.product + ", quantity=" + this.quantity + ", orderPrice=" + this.orderPrice + ")";
        }
    }

    public static OrderProductsBuilder builder() {
        return new OrderProductsBuilder();
    }
}
