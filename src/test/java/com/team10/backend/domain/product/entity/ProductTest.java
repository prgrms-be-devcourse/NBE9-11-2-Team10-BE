package com.team10.backend.domain.product.entity;

import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.enums.UserStatus;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private User createSeller() {
        return User.builder()
                .email("seller@test.com")
                .password("1234")
                .name("테스트판매자")
                .nickname("seller1")
                .phoneNumber("010-1234-5678")
                .address("서울시")
                .userStatus(UserStatus.ACTIVE)
                .role(Role.SELLER)
                .build();
    }

    @Test
    @DisplayName("재고 차감 성공")
    void decreaseStock_success() {
        Product product = new Product(
                createSeller(),
                ProductType.BOOK,
                "상품명",
                "설명",
                10000,
                10,
                null
        );

        product.decreaseStock(3);

        assertThat(product.getStock()).isEqualTo(7);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    @DisplayName("재고를 모두 차감하면 상태가 SOLD_OUT으로 변경")
    void decreaseStock_success_soldOutWhenZero() {
        Product product = new Product(
                createSeller(),
                ProductType.BOOK,
                "상품명",
                "설명",
                10000,
                3,
                null
        );

        product.decreaseStock(3);

        assertThat(product.getStock()).isEqualTo(0);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("재고보다 많이 차감하면 예외 발생")
    void decreaseStock_fail_insufficientStock() {
        Product product = new Product(
                createSeller(),
                ProductType.BOOK,
                "상품명",
                "설명",
                10000,
                3,
                null
        );

        assertThatThrownBy(() -> product.decreaseStock(5))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());
    }

    @Test
    @DisplayName("재고 증가 성공")
    void increaseStock_success() {
        Product product = new Product(
                createSeller(),
                ProductType.BOOK,
                "상품명",
                "설명",
                10000,
                0,
                null
        );

        product.increaseStock(5);

        assertThat(product.getStock()).isEqualTo(5);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    @DisplayName("0 이하 수량 증가 요청 시 예외가 발생한다")
    void increaseStock_fail_invalidQuantity() {
        Product product = new Product(
                createSeller(),
                ProductType.BOOK,
                "상품명",
                "설명",
                10000,
                10,
                null
        );

        assertThatThrownBy(() -> product.increaseStock(0))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("0 이하 수량 차감 요청 시 예외가 발생한다")
    void decreaseStock_fail_invalidQuantity() {
        Product product = new Product(
                createSeller(),
                ProductType.BOOK,
                "상품명",
                "설명",
                10000,
                10,
                null
        );

        assertThatThrownBy(() -> product.decreaseStock(0))
                .isInstanceOf(BusinessException.class);
    }
}