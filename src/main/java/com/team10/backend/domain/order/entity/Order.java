package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.order.enums.OrderStatus;
import com.team10.backend.domain.order.enums.PaymentStatus;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@SQLDelete(sql = "UPDATE orders SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false") // 삭제 시 is_deleted 필드를 true로 UPDATE, 조회 할 때, true값 필터링 수행
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 외부(토스 등)에 노출할 고유 주문 번호q
    @Column(name = "order_number", nullable = false, unique = true)//유니크로 설정
    private String orderNumber;

    @Column(name = "total_amount")
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status; // PENDING, SUCCESS, CANCELLED

    @Column(name = "is_deleted")
    private boolean isDeleted;

    private Order(User user, String orderNumber, int totalAmount) {
        this.user = user;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING; // 생성 시 기본값
        this.isDeleted = false;
    }

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProducts> orderProducts = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderDelivery delivery;

    // 추가: 결제 이력을 관리하기 위한 리스트 (1:N)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    // == 생성 메서드 ==
    public static Order createOrder(User user, String orderNumber, OrderDelivery delivery, List<OrderProducts> items) {
        //총 합을 구한다.
        int calculatedTotalAmount = items.stream()
                .mapToInt(item -> item.getOrderPrice() * item.getQuantity())
                .sum();

        //오더 생성
        Order order = Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .totalAmount(calculatedTotalAmount)
                .build();

        // 양방향 관계 설정
        order.setDelivery(delivery);
        items.forEach(order::addOrderProduct);

        // 추가: 주문 생성 시점에 초기 결제(Payment) 객체도 함께 생성하여 포함시킴
        Payment initialPayment = Payment.builder()
                .order(order)
                .orderNumber(orderNumber)
                .totalAmount(calculatedTotalAmount) // Payment 설계에 맞게 Long 형변환
                .status(PaymentStatus.READY)
                .idempotencyKey(null)//임시로 null
                .build();
        order.addPayment(initialPayment);

        return order;
    }

    // == 연관관계 편의 메서드 ==
    private void setDelivery(OrderDelivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }

    private void addOrderProduct(OrderProducts orderProduct) {
        this.orderProducts.add(orderProduct);
        orderProduct.setOrder(this);
    }

    public void addPayment(Payment payment) {
        this.payments.add(payment);
        // Payment 엔티티에 setOrder 메서드가 필요합니다 (OrderDelivery처럼)
        payment.setOrder(this);
    }

    public void cancelStatusOrder() {
        // 만약 별도의 OrderStatus 필드가 있다면 CANCEL로 변경
        this.status = OrderStatus.CANCELED;
    }

    public void successStatusOrder() {
        // 만약 별도의 OrderStatus 필드가 있다면 CANCEL로 변경
        this.status = OrderStatus.SUCCESS;
    }

    public User getUser() {
        return this.user;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public int getTotalAmount() {
        return this.totalAmount;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public boolean isDeleted() {
        return this.isDeleted;
    }

    public List<OrderProducts> getOrderProducts() {
        return this.orderProducts;
    }

    public OrderDelivery getDelivery() {
        return this.delivery;
    }

    public List<Payment> getPayments() {
        return this.payments;
    }

    protected Order() {
    }

    public static class OrderBuilder {
        private User user;
        private String orderNumber;
        private int totalAmount;

        OrderBuilder() {
        }

        public OrderBuilder user(User user) {
            this.user = user;
            return this;
        }

        public OrderBuilder orderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public OrderBuilder totalAmount(int totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Order build() {
            return new Order(this.user, this.orderNumber, this.totalAmount);
        }

        public String toString() {
            return "Order.OrderBuilder(user=" + this.user + ", orderNumber=" + this.orderNumber + ", totalAmount=" + this.totalAmount + ")";
        }
    }

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }
}
