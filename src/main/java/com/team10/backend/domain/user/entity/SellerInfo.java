package com.team10.backend.domain.user.entity;

import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seller_info")
@Getter
@NoArgsConstructor
public class SellerInfo extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column()
    private String bio;

    @Column(name = "business_number", unique = true)
    private String businessNumber;

    public void linkUser(User user) {
        this.user = user;
    }

}
