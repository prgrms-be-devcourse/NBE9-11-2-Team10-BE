package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order extends BaseEntity {

    @ManyToOne
    private User user;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name="total_amount")
    private int totalAmount;

    @Column(name="delivery_address")
    private String deliveryAddress;

}
