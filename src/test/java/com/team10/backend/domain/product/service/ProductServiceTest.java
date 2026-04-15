package com.team10.backend.domain.product.service;

import com.team10.backend.domain.product.dto.ProductCreateRequest;
import com.team10.backend.domain.product.dto.ProductPageResponse;
import com.team10.backend.domain.product.dto.ProductResponse;
import com.team10.backend.domain.product.entity.Product;
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

        ProductCreateRequest request = new ProductCreateRequest("ABC", "책 설명입니다.", 10000, 100, null, ProductType.BOOK);
        ProductResponse response = productService.create(user, request);

        assertThat(response.productId()).isNotNull();
        assertThat(response.productName()).isEqualTo("ABC");
        assertThat(response.description()).isEqualTo("책 설명입니다.");
        assertThat(response.price()).isEqualTo(10000);
        assertThat(response.stock()).isEqualTo(100);
        assertThat(response.type()).isEqualTo(ProductType.BOOK);
        assertThat(response.imageUrl()).isNull();
        assertThat(response.status()).isEqualTo(ProductStatus.SELLING);
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("상품 전체 조회 시, 상품 목록과 페이지 정보 반환")
    void listProducts_withPaging() {
        User user = userRepository.findById(1L).orElseThrow();

        productRepository.save(new Product(user, ProductType.BOOK, "책1", "설명1", 10000, 10, null));
        productRepository.save(new Product(user, ProductType.BOOK, "책2", "설명2", 20000, 20, null));

        ProductPageResponse response = productService.list(0, 10, null, null);

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("상품 전체 조회 시, type 필터로 상품 조회")
    void listProducts_withTypeFilter() {
        User user = userRepository.findById(1L).orElseThrow();

        productRepository.save(new Product(user, ProductType.BOOK, "책1", "설명1", 10000, 10, null));
        productRepository.save(new Product(user, ProductType.EBOOK, "굿즈1", "설명2", 20000, 20, null));

        ProductPageResponse response = productService.list(0, 10, ProductType.BOOK, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).productName()).isEqualTo("책1");
        assertThat(response.content().get(0).type()).isEqualTo(ProductType.BOOK);
    }

    @Test
    @DisplayName("상품 전체 조회 시, status 필터로 상품 조회")
    void listProducts_withStatusFilter() {
        User user = userRepository.findById(1L).orElseThrow();

        productRepository.save(new Product(user, ProductType.BOOK, "책1", "설명1", 10000, 10, null));

        Product inactiveProduct = new Product(user, ProductType.EBOOK, "전자책1", "설명2", 20000, 20, null);
        inactiveProduct.updateStatus(ProductStatus.INACTIVE);
        productRepository.save(inactiveProduct);

        ProductPageResponse response = productService.list(0, 10, null, ProductStatus.SELLING);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).productName()).isEqualTo("책1");
        assertThat(response.content().get(0).status()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    @DisplayName("상품 전체 조회 시, type과 status 필터로 상품 조회")
    void listProducts_withTypeAndStatusFilter() {
        User user = userRepository.findById(1L).orElseThrow();

        productRepository.save(new Product(user, ProductType.BOOK, "책1", "설명1", 10000, 10, null));

        Product inactiveBook = new Product(user, ProductType.BOOK, "책2", "설명2", 15000, 5, null);
        inactiveBook.updateStatus(ProductStatus.INACTIVE);
        productRepository.save(inactiveBook);

        productRepository.save(new Product(user, ProductType.EBOOK, "전자책1", "설명3", 20000, 20, null));

        ProductPageResponse response = productService.list(0, 10, ProductType.BOOK, ProductStatus.SELLING);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).productName()).isEqualTo("책1");
        assertThat(response.content().get(0).type()).isEqualTo(ProductType.BOOK);
        assertThat(response.content().get(0).status()).isEqualTo(ProductStatus.SELLING);
    }
}