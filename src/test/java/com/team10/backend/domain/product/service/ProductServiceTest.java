package com.team10.backend.domain.product.service;

import com.team10.backend.domain.product.dto.ProductCreateRequest;
import com.team10.backend.domain.product.dto.ProductDetailResponse;
import com.team10.backend.domain.product.dto.ProductInactiveResponse;
import com.team10.backend.domain.product.dto.ProductPageResponse;
import com.team10.backend.domain.product.dto.ProductUpdateRequest;
import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;
import com.team10.backend.domain.product.repository.ProductRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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
        ProductCreateRequest request = new ProductCreateRequest("ABC", "책 설명입니다.", 10000, 100, null, ProductType.BOOK);

        ProductDetailResponse response = productService.create(1L, request);

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

    @Test
    @DisplayName("상품 상세 조회 성공")
    void detail_success() {
        User user = userRepository.findById(1L).orElseThrow();

        Product savedProduct = productRepository.save(new Product(
                user,
                ProductType.BOOK,
                "ABC",
                "책 설명입니다.",
                10000,
                100,
                null
        ));

        ProductDetailResponse response = productService.detail(savedProduct.getId());

        assertThat(response.productId()).isEqualTo(savedProduct.getId());
        assertThat(response.productName()).isEqualTo("ABC");
        assertThat(response.description()).isEqualTo("책 설명입니다.");
        assertThat(response.price()).isEqualTo(10000);
        assertThat(response.stock()).isEqualTo(100);
        assertThat(response.type()).isEqualTo(ProductType.BOOK);
        assertThat(response.imageUrl()).isNull();
        assertThat(response.status()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    @DisplayName("존재하지 않는 상품 상세 조회 시, 예외 발생")
    void detail_fail_productNotFound() {
        assertThatThrownBy(() -> productService.detail(9999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateProduct_success() {
        User user = userRepository.findById(1L).orElseThrow();

        Product savedProduct = productRepository.save(new Product(
                user,
                ProductType.BOOK,
                "기존 상품명",
                "기존 설명",
                10000,
                10,
                "https://example.com/old.jpg"
        ));

        ProductUpdateRequest request = new ProductUpdateRequest(
                "수정된 상품명",
                "수정된 설명",
                12000,
                20,
                "https://example.com/new.jpg",
                ProductType.EBOOK,
                ProductStatus.SOLD_OUT
        );

        ProductDetailResponse response = productService.update(savedProduct.getId(), request);

        assertThat(response.productId()).isEqualTo(savedProduct.getId());
        assertThat(response.productName()).isEqualTo("수정된 상품명");
        assertThat(response.description()).isEqualTo("수정된 설명");
        assertThat(response.price()).isEqualTo(12000);
        assertThat(response.stock()).isEqualTo(20);
        assertThat(response.imageUrl()).isEqualTo("https://example.com/new.jpg");
        assertThat(response.type()).isEqualTo(ProductType.EBOOK);
        assertThat(response.status()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("존재하지 않는 상품 수정 시, 예외 발생")
    void updateProduct_fail_productNotFound() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "수정된 상품명",
                "수정된 설명",
                12000,
                20,
                "https://example.com/new.jpg",
                ProductType.BOOK,
                ProductStatus.SELLING
        );

        assertThatThrownBy(() -> productService.update(9999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("상품 비활성화 성공")
    void inactiveProduct_success() {
        User user = userRepository.findById(1L).orElseThrow();

        Product savedProduct = productRepository.save(new Product(
                user,
                ProductType.BOOK,
                "비활성화 대상 상품",
                "상품 설명",
                10000,
                10,
                "https://example.com/book.jpg"
        ));

        ProductInactiveResponse response = productService.inactive(savedProduct.getId());

        assertThat(response.productId()).isEqualTo(savedProduct.getId());
        assertThat(response.status()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(response.message()).isEqualTo("상품이 삭제되었습니다.");

        Product product = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("이미 비활성화된 상품 재요청 시 예외 발생")
    void inactiveProduct_fail_alreadyInactive() {
        User user = userRepository.findById(1L).orElseThrow();

        Product savedProduct = productRepository.save(new Product(
                user,
                ProductType.BOOK,
                "이미 비활성화된 상품",
                "상품 설명",
                10000,
                10,
                "https://example.com/book.jpg"
        ));

        savedProduct.updateStatus(ProductStatus.INACTIVE);

        assertThatThrownBy(() -> productService.inactive(savedProduct.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_ALREADY_INACTIVE.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 상품 비활성화 시, 예외 발생")
    void inactiveProduct_fail_productNotFound() {
        assertThatThrownBy(() -> productService.inactive(9999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }
}