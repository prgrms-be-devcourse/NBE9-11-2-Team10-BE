package com.team10.backend.domain.user.entity;

import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "seller_info")
public class SellerInfo extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(length = 500)
    private String bio;

    @Column(name = "business_number", unique = true)
    private String businessNumber;

    public void linkUser(User user) {
        this.user = user;
    }

    public void updateSellerInfo(String bio, String businessNumber) {
        this.bio = bio;
        this.businessNumber = businessNumber;
    }

    public User getUser() {
        return this.user;
    }

    public String getBio() {
        return this.bio;
    }

    public String getBusinessNumber() {
        return this.businessNumber;
    }

    public SellerInfo() {
    }
}
