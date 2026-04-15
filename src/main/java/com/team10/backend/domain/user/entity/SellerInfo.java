package com.team10.backend.domain.user.entity;

import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seller_info")
@Getter
@NoArgsConstructor
public class SellerInfo extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_number", nullable = false, unique = true)
    private String businessNumber;
}