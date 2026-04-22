package com.team10.backend.domain.product.controller;

import com.team10.backend.domain.product.dto.ProductStockRequest;
import com.team10.backend.domain.product.dto.ProductUpdateRequest;
import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;
import com.team10.backend.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

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

        jdbcTemplate.update(
                "INSERT INTO users " +
                        "(id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                3L,
                "buyer@test.com",
                "1234",
                "테스트구매자",
                "buyer1",
                "010-1111-2222",
                "대구시",
                "ACTIVE",
                "BUYER"
        );
    }

    @Test
    @DisplayName("비로그인 사용자의 상품 등록 요청은 401")
    void createProduct_fail_unauthorized() throws Exception {
        String requestBody = """
                {
                  "productName": "ABC",
                  "price": 10000,
                  "stock": 100,
                  "type": "BOOK"
                }
                """;

        mockMvc.perform(post("/api/v1/stores/me/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("BUYER 권한 사용자의 상품 등록 요청은 403")
    @WithMockUser(username = "3", roles = "BUYER")
    void createProduct_fail_forbidden() throws Exception {
        String requestBody = """
                {
                  "productName": "ABC",
                  "price": 10000,
                  "stock": 100,
                  "imageUrl": "https://www.exam.com/product.jpg",
                  "type": "BOOK"
                }
                """;

        mockMvc.perform(post("/api/v1/stores/me/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("상품 등록 성공")
    @WithMockUser(username = "1", roles = "SELLER")
    void createProduct_success() throws Exception {
        String requestBody = """
                {
                  "productName": "ABC",
                  "price": 10000,
                  "stock": 100,
                  "imageUrl": "https://www.exam.com/product.jpg",
                  "type": "BOOK"
                }
                """;

        mockMvc.perform(post("/api/v1/stores/me/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").exists())
                .andExpect(jsonPath("$.data.productName").value("ABC"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(100))
                .andExpect(jsonPath("$.data.imageUrl").value("https://www.exam.com/product.jpg"))
                .andExpect(jsonPath("$.data.type").value("BOOK"))
                .andExpect(jsonPath("$.data.status").value("SELLING"));

        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);

        Product product = products.get(0);
        assertThat(product.getProductName()).isEqualTo("ABC");
        assertThat(product.getPrice()).isEqualTo(10000);
        assertThat(product.getStock()).isEqualTo(100);
        assertThat(product.getImageUrl()).isEqualTo("https://www.exam.com/product.jpg");
        assertThat(product.getType()).isEqualTo(ProductType.BOOK);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SELLING);
        assertThat(product.getUser().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("상품 수정 성공")
    @WithMockUser(username = "1", roles = "SELLER")
    void updateProduct_success() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO products " +
                        "(id, user_id, type, product_name, description, price, stock, image_url, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                1L,
                1L,
                "BOOK",
                "기존 상품명",
                "기존 설명",
                10000,
                10,
                "https://www.exam.com/oldimg",
                "SELLING"
        );

        ProductUpdateRequest request = new ProductUpdateRequest(
                "ABC 수정본",
                "상품 수정",
                12000,
                "https://www.exam.com/bookimg",
                ProductType.BOOK,
                ProductStatus.SELLING
        );

        mockMvc.perform(put("/api/v1/stores/me/products/{productId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.productName").value("ABC 수정본"))
                .andExpect(jsonPath("$.data.description").value("상품 수정"))
                .andExpect(jsonPath("$.data.price").value(12000))
                .andExpect(jsonPath("$.data.stock").value(10))
                .andExpect(jsonPath("$.data.imageUrl").value("https://www.exam.com/bookimg"))
                .andExpect(jsonPath("$.data.type").value("BOOK"))
                .andExpect(jsonPath("$.data.status").value("SELLING"));

        Product product = productRepository.findById(1L).orElseThrow();
        assertThat(product.getProductName()).isEqualTo("ABC 수정본");
        assertThat(product.getDescription()).isEqualTo("상품 수정");
        assertThat(product.getPrice()).isEqualTo(12000);
        assertThat(product.getStock()).isEqualTo(10);
        assertThat(product.getImageUrl()).isEqualTo("https://www.exam.com/bookimg");
        assertThat(product.getType()).isEqualTo(ProductType.BOOK);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    @DisplayName("본인 상품이 아닌 상품 수정 요청은 403")
    @WithMockUser(username = "2", roles = "SELLER")
    void updateProduct_fail_accessDenied() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO products " +
                        "(id, user_id, type, product_name, description, price, stock, image_url, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                1L,
                1L,
                "BOOK",
                "기존 상품명",
                "기존 설명",
                10000,
                10,
                "https://www.exam.com/oldimg",
                "SELLING"
        );

        ProductUpdateRequest request = new ProductUpdateRequest(
                "ABC 수정본",
                "상품 수정",
                12000,
                "https://www.exam.com/bookimg",
                ProductType.BOOK,
                ProductStatus.SELLING
        );

        mockMvc.perform(put("/api/v1/stores/me/products/{productId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COMMON_003"));
    }

    @Test
    @DisplayName("상품 비활성화 성공")
    @WithMockUser(username = "1", roles = "SELLER")
    void inactiveProduct_success() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO products " +
                        "(id, user_id, type, product_name, description, price, stock, image_url, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                1L,
                1L,
                "BOOK",
                "비활성화 대상 상품",
                "상품 설명",
                10000,
                10,
                "https://www.exam.com/bookimg",
                "SELLING"
        );

        mockMvc.perform(patch("/api/v1/stores/me/products/{productId}/inactive", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.message").value("상품이 삭제되었습니다."));

        Product product = productRepository.findById(1L).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("이미 비활성화된 상품 재요청 시 409")
    @WithMockUser(username = "1", roles = "SELLER")
    void inactiveProduct_fail_alreadyInactive() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO products " +
                        "(id, user_id, type, product_name, description, price, stock, image_url, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                1L,
                1L,
                "BOOK",
                "이미 비활성화된 상품",
                "상품 설명",
                10000,
                10,
                "https://www.exam.com/bookimg",
                "INACTIVE"
        );

        mockMvc.perform(patch("/api/v1/stores/me/products/{productId}/inactive", 1L))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("재고 수정 성공")
    @WithMockUser(username = "1", roles = "SELLER")
    void updateStock_success() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO products " +
                        "(id, user_id, type, product_name, description, price, stock, image_url, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                1L,
                1L,
                "BOOK",
                "기존 상품명",
                "기존 설명",
                10000,
                10,
                "https://www.exam.com/oldimg",
                "SELLING"
        );

        ProductStockRequest request = new ProductStockRequest(30);

        mockMvc.perform(patch("/api/v1/stores/me/products/{productId}/stock", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.stock").value(30))
                .andExpect(jsonPath("$.data.message").value("상품 재고가 수정되었습니다."));
    }

    @Test
    @DisplayName("재고를 음수로 수정하면 검증 실패")
    @WithMockUser(username = "1", roles = "SELLER")
    void updateStock_fail_negativeStock() throws Exception {
        ProductStockRequest request = new ProductStockRequest(-1);

        mockMvc.perform(patch("/api/v1/stores/me/products/{productId}/stock", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
