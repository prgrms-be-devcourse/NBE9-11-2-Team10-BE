package com.team10.backend.domain.product.service;

import com.team10.backend.domain.image.service.ImageUploadService;
import com.team10.backend.domain.product.dto.ProductCreateRequest;
import com.team10.backend.domain.product.dto.ProductDetailResponse;
import com.team10.backend.domain.product.dto.ProductInactiveResponse;
import com.team10.backend.domain.product.dto.ProductListResponse;
import com.team10.backend.domain.product.dto.ProductPageResponse;
import com.team10.backend.domain.product.dto.ProductStockRequest;
import com.team10.backend.domain.product.dto.ProductStockResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @MockitoBean
    private ImageUploadService imageUploadService;

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

        jdbcTemplate.update(
                "INSERT INTO users " +
                        "(id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                2L,
                "seller2@test.com",
                "1234",
                "테스트판매자2",
                "seller2",
                "010-9999-8888",
                "부산시",
                "ACTIVE",
                "SELLER"
        );
    }

    @Test
    @DisplayName("상품 생성 시, SELLING 상태")
    void createProduct_defaultStatusSelling() {
        ProductCreateRequest request = new ProductCreateRequest(
                "ABC",
                "책 설명입니다.",
                10000,
                100,
                null,
                ProductType.BOOK
        );

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
    @DisplayName("상품 생성 시 imageUrl이 있으면 저장")
    void createProduct_withImageUrl() {
        ProductCreateRequest request = new ProductCreateRequest(
                "이미지 상품",
                "이미지 있는 상품입니다.",
                10000,
                100,
                "https://example.com/product.jpg",
                ProductType.BOOK
        );

        ProductDetailResponse response = productService.create(1L, request);

        assertThat(response.productName()).isEqualTo("이미지 상품");
        assertThat(response.imageUrl()).isEqualTo("https://example.com/product.jpg");

        Product product = productRepository.findById(response.productId()).orElseThrow();
        assertThat(product.getImageUrl()).isEqualTo("https://example.com/product.jpg");
    }

    @Test
    @DisplayName("상품 전체 조회 시, 상품 목록과 페이지 정보 반환")
    void listProducts_withPaging() {
        User user = userRepository.findById(1L).orElseThrow();

        productRepository.save(new Product(user, ProductType.BOOK, "책1", "설명1", 10000, 10, null));
        productRepository.save(new Product(user, ProductType.BOOK, "책2", "설명2", 20000, 20, null));

        ProductPageResponse response = productService.list(0, 10, null, null, null);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).nickname()).isEqualTo("seller1");
        assertThat(response.page()).isEqualTo(1);
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

        ProductPageResponse response = productService.list(0, 10, ProductType.BOOK, null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).nickname()).isEqualTo("seller1");
        assertThat(response.content().get(0).productName()).isEqualTo("책1");
        assertThat(response.content().get(0).type()).isEqualTo(ProductType.BOOK);
    }

    @Test
    @DisplayName("상품 전체 조회 시, status 필터로 상품 조회")
    void listProducts_withStatusFilter() {
        User user = userRepository.findById(1L).orElseThrow();

        productRepository.save(new Product(user, ProductType.BOOK, "책1", "설명1", 10000, 10, null));

        Product inactiveProduct = new Product(user, ProductType.EBOOK, "전자책1", "설명2", 20000, 20, null);
        inactiveProduct.inactivate();
        productRepository.save(inactiveProduct);

        ProductPageResponse response = productService.list(0, 10, null, ProductStatus.SELLING, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).nickname()).isEqualTo("seller1");
        assertThat(response.content().get(0).productName()).isEqualTo("책1");
        assertThat(response.content().get(0).status()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    @DisplayName("상품 전체 조회 시, type과 status 필터로 상품 조회")
    void listProducts_withTypeAndStatusFilter() {
        User user = userRepository.findById(1L).orElseThrow();

        productRepository.save(new Product(user, ProductType.BOOK, "책1", "설명1", 10000, 10, null));

        Product inactiveBook = new Product(user, ProductType.BOOK, "책2", "설명2", 15000, 5, null);
        inactiveBook.inactivate();
        productRepository.save(inactiveBook);

        productRepository.save(new Product(user, ProductType.EBOOK, "전자책1", "설명3", 20000, 20, null));

        ProductPageResponse response = productService.list(0, 10, ProductType.BOOK, ProductStatus.SELLING, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).nickname()).isEqualTo("seller1");
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
        assertThat(response.nickname()).isEqualTo("seller1");
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
                "https://example.com/new.jpg",
                ProductType.EBOOK,
                ProductStatus.SOLD_OUT
        );

        ProductDetailResponse response = productService.update(1L, savedProduct.getId(), request);

        assertThat(response.productId()).isEqualTo(savedProduct.getId());
        assertThat(response.productName()).isEqualTo("수정된 상품명");
        assertThat(response.description()).isEqualTo("수정된 설명");
        assertThat(response.price()).isEqualTo(12000);
        assertThat(response.imageUrl()).isEqualTo("https://example.com/new.jpg");
        assertThat(response.type()).isEqualTo(ProductType.EBOOK);
        assertThat(response.status()).isEqualTo(ProductStatus.SOLD_OUT);
        verify(imageUploadService).deleteIfManaged("https://example.com/old.jpg");
    }

    @Test
    @DisplayName("상품 수정 시 imageUrl이 null이면 상품 이미지 삭제 - 성공")
    void updateProduct_deleteImageWhenImageUrlIsNull_success() {
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
                null,
                ProductType.EBOOK,
                ProductStatus.SELLING
        );

        ProductDetailResponse response = productService.update(1L, savedProduct.getId(), request);

        assertThat(response.productId()).isEqualTo(savedProduct.getId());
        assertThat(response.imageUrl()).isNull();
        verify(imageUploadService).deleteIfManaged("https://example.com/old.jpg");
    }

    @Test
    @DisplayName("상품 수정 시 imageUrl이 같으면 기존 이미지 삭제하지 않음")
    void updateProduct_skipImageDeleteWhenImageUrlIsSame_success() {
        User user = userRepository.findById(1L).orElseThrow();

        Product savedProduct = productRepository.save(new Product(
                user,
                ProductType.BOOK,
                "기존 상품명",
                "기존 설명",
                10000,
                10,
                "https://example.com/same.jpg"
        ));

        ProductUpdateRequest request = new ProductUpdateRequest(
                "수정된 상품명",
                "수정된 설명",
                12000,
                "https://example.com/same.jpg",
                ProductType.EBOOK,
                ProductStatus.SELLING
        );

        ProductDetailResponse response = productService.update(1L, savedProduct.getId(), request);

        assertThat(response.productId()).isEqualTo(savedProduct.getId());
        assertThat(response.imageUrl()).isEqualTo("https://example.com/same.jpg");
        verify(imageUploadService, never()).deleteIfManaged("https://example.com/same.jpg");
    }

    @Test
    @DisplayName("존재하지 않는 상품 수정 시, 예외 발생")
    void updateProduct_fail_productNotFound() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "수정된 상품명",
                "수정된 설명",
                12000,
                "https://example.com/new.jpg",
                ProductType.BOOK,
                ProductStatus.SELLING
        );

        assertThatThrownBy(() -> productService.update(1L, 9999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("본인 상품이 아닌 상품 수정 시, 예외 발생")
    void updateProduct_fail_accessDenied() {
        User owner = userRepository.findById(1L).orElseThrow();

        Product savedProduct = productRepository.save(new Product(
                owner,
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
                "https://example.com/new.jpg",
                ProductType.EBOOK,
                ProductStatus.SELLING
        );

        assertThatThrownBy(() -> productService.update(2L, savedProduct.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
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

        ProductInactiveResponse response = productService.inactive(1L, savedProduct.getId());

        assertThat(response.productId()).isEqualTo(savedProduct.getId());
        assertThat(response.status()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(response.message()).isEqualTo("상품이 삭제되었습니다.");

        Product product = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("이미 비활성화된 상품 재요청 시, 예외 발생")
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

        savedProduct.inactivate();

        assertThatThrownBy(() -> productService.inactive(1L, savedProduct.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_ALREADY_INACTIVE.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 상품 비활성화 시, 예외 발생")
    void inactiveProduct_fail_productNotFound() {
        assertThatThrownBy(() -> productService.inactive(1L, 9999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("본인 상품이 아닌 상품 비활성화 시, 예외 발생")
    void inactiveProduct_fail_accessDenied() {
        User owner = userRepository.findById(1L).orElseThrow();

        Product savedProduct = productRepository.save(new Product(
                owner,
                ProductType.BOOK,
                "비활성화 대상 상품",
                "상품 설명",
                10000,
                10,
                "https://example.com/book.jpg"
        ));

        assertThatThrownBy(() -> productService.inactive(2L, savedProduct.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("재고 수정 성공")
    void updateStock_success() {
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

        ProductStockRequest request = new ProductStockRequest(30);

        ProductStockResponse response = productService.updateStock(1L, savedProduct.getId(), request);

        assertThat(response.productId()).isEqualTo(savedProduct.getId());
        assertThat(response.stock()).isEqualTo(30);
        assertThat(response.message()).isEqualTo("상품 재고가 수정되었습니다.");

        Product product = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(product.getStock()).isEqualTo(30);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    @DisplayName("존재하지 않는 상품 재고 수정 시, 예외 발생")
    void updateStock_fail_productNotFound() {
        ProductStockRequest request = new ProductStockRequest(30);

        assertThatThrownBy(() -> productService.updateStock(1L, 9999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("비활성화된 상품 재고 수정 시, 예외 발생")
    void updateStock_fail_inactiveProduct() {
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

        savedProduct.inactivate();

        ProductStockRequest request = new ProductStockRequest(30);

        assertThatThrownBy(() -> productService.updateStock(1L, savedProduct.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_ALREADY_INACTIVE.getMessage());
    }

    @Test
    @DisplayName("본인 상품이 아닌 상품 재고 수정 시, 예외 발생")
    void updateStock_fail_accessDenied() {
        User owner = userRepository.findById(1L).orElseThrow();

        Product savedProduct = productRepository.save(new Product(
                owner,
                ProductType.BOOK,
                "기존 상품명",
                "기존 설명",
                10000,
                10,
                "https://example.com/old.jpg"
        ));

        ProductStockRequest request = new ProductStockRequest(30);

        assertThatThrownBy(() -> productService.updateStock(2L, savedProduct.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("sellerId로 상품 필터링")
    void list_withSellerId_filters() {
        User seller = userRepository.findById(1L).orElseThrow();
        User anotherSeller = userRepository.findById(2L).orElseThrow();

        productRepository.save(new Product(
                seller,
                ProductType.BOOK,
                "seller 상품",
                "설명",
                10000,
                10,
                "https://example.com/seller.jpg"
        ));

        productRepository.save(new Product(
                anotherSeller,
                ProductType.BOOK,
                "다른 판매자 상품",
                "설명",
                12000,
                10,
                "https://example.com/another.jpg"
        ));

        ProductPageResponse response = productService.list(0, 10, null, null, seller.getId());

        assertThat(response.content()).isNotEmpty();
        assertThat(response.content())
                .extracting(ProductListResponse::sellerId)
                .containsOnly(seller.getId());
    }

    @Test
    @DisplayName("재고를 0으로 수정하면 상태가 SOLD_OUT으로 변경된다")
    void updateStock_success_soldOutWhenZero() {
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

        ProductStockRequest request = new ProductStockRequest(0);

        ProductStockResponse response = productService.updateStock(1L, savedProduct.getId(), request);

        assertThat(response.productId()).isEqualTo(savedProduct.getId());
        assertThat(response.stock()).isEqualTo(0);

        Product product = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(product.getStock()).isEqualTo(0);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("SOLD_OUT 상품의 재고가 0에서 1 이상으로 오르면 SELLING으로 변경")
    void updateStock_success_sellingWhenStockBecomesPositive() {
        User user = userRepository.findById(1L).orElseThrow();

        Product soldOutProduct = productRepository.save(new Product(
                user,
                ProductType.BOOK,
                "품절 상품",
                "설명",
                10000,
                0,
                "https://example.com/book.jpg"
        ));

        ProductStockRequest request = new ProductStockRequest(5);

        ProductStockResponse response = productService.updateStock(1L, soldOutProduct.getId(), request);

        assertThat(response.productId()).isEqualTo(soldOutProduct.getId());
        assertThat(response.stock()).isEqualTo(5);

        Product product = productRepository.findById(soldOutProduct.getId()).orElseThrow();
        assertThat(product.getStock()).isEqualTo(5);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    @DisplayName("음수 재고 입력 시, 예외 발생")
    void updateStock_fail_invalidStock() {
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

        ProductStockRequest request = new ProductStockRequest(-1);

        assertThatThrownBy(() -> productService.updateStock(1L, savedProduct.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_STOCK.getMessage());
    }
}
