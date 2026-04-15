package com.team10.backend.domain.product.service;

import com.team10.backend.domain.product.dto.ProductCreateRequest;
import com.team10.backend.domain.product.dto.ProductResponse;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;
import com.team10.backend.domain.product.repository.ProductRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update(
                "INSERT INTO users " +
                        "(id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                1L,
                "seller@test.com",
                "1234",
                "테스트판매자",
                "seller1",
                "010-1234-5678",
                "서울시",
                "ACTIVE",
                "SELLER"
        );
    }

    @Test
    @DisplayName("상품 생성 시 SELLING 상태")
    void createProduct_defaultStatusSelling() {
        User user = userRepository.findById(1L).orElseThrow();

        ProductCreateRequest request = new ProductCreateRequest("ABC", 10000, 100, ProductType.BOOK);
        ProductResponse response = productService.create(user, request);

        assertThat(response.productId()).isNotNull();
        assertThat(response.productName()).isEqualTo("ABC");
        assertThat(response.price()).isEqualTo(10000);
        assertThat(response.stock()).isEqualTo(100);
        assertThat(response.type()).isEqualTo(ProductType.BOOK);
        assertThat(response.status()).isEqualTo(ProductStatus.SELLING);
        assertThat(productRepository.count()).isEqualTo(1);
    }
}