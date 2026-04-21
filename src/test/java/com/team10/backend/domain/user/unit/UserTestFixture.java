package com.team10.backend.domain.user.unit;

import com.team10.backend.domain.user.entity.SellerInfo;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.enums.UserStatus;

public class UserTestFixture {
    public static User createBuyer() {
        return User.builder()
                .email("buyer@test.com")
                .password("password123!")
                .name("김구매")
                .nickname("buyer")
                .phoneNumber("010-0000-0000")
                .address("서울시 동대문구")
                .userStatus(UserStatus.ACTIVE)
                .role(Role.BUYER)
                .build();
    }

    public static User createSeller() {
        User user = User.builder()
                .email("seller@test.com")
                .password("password123!")
                .name("송판매")
                .nickname("seller")
                .phoneNumber("010-1111-1111")
                .address("서울시 동대문구")
                .userStatus(UserStatus.ACTIVE)
                .role(Role.SELLER)
                .build();
        SellerInfo sellerInfo = new SellerInfo();
        sellerInfo.updateSellerInfo("안녕하세요", "123-123-12345");
        user.attachSellerInfo(sellerInfo);

        return user;
    }

}
