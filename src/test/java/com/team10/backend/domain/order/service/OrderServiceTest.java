package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.OrderCreateRequest;
import com.team10.backend.domain.order.dto.OrderResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // 1. 기존 데이터 정리 (자식 테이블부터 삭제하여 외래키 제약조건 위반 방지)
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM order_products");
        jdbcTemplate.update("DELETE FROM order_delivery");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");

        // 2. 유저 데이터 삽입 (ID: 1)
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                1L, "buyer@test.com", "1234", "홍길동", "길동이", "010-1234-5678", "서울시 강남구", "ACTIVE", "BUYER"
        );

        // 3. 상품 데이터 삽입 (ID: 101, 102)
        // t1 테스트에서 101번과 102번 상품을 모두 사용하므로 두 개 다 넣어줍니다.
        jdbcTemplate.update(
                "INSERT INTO products (id, user_id, product_name, price, stock, type, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                101L, 1L, "맥북 프로", 2000000, 10, "BOOK", "SELLING"
        );

        jdbcTemplate.update(
                "INSERT INTO products (id, user_id, product_name, price, stock, type, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                102L, 1L, "매직 마우스", 50000, 20, "BOOK", "SELLING"
        );
    }

    @Test
    @DisplayName("주문 생성 서비스 - 모든 연관 엔티티가 DB에 저장되어야 한다 (Cascade 확인)/ order에 연관된 DB에 값들이 잘 들어가는 지 확인")
    void t1() {
        // Given
        // 아이템 1: 맥북 프로(2,000,000원) x 1 = 2,000,000
        OrderCreateRequest.OrderProductReq item1 = new OrderCreateRequest.OrderProductReq(101L, 1);
        // 아이템 2: 매직 마우스(50,000원) x 2 = 100,000
        OrderCreateRequest.OrderProductReq item2 = new OrderCreateRequest.OrderProductReq(102L, 2);

        OrderCreateRequest req = new OrderCreateRequest("서울시 강남구 테헤란로", List.of(item1, item2));

        // When
        OrderResponse response = orderService.createOrder(1L,req);

        entityManager.flush();
        entityManager.clear();

        // Then 1: 응답 데이터 검증
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.totalAmount()).isEqualTo(2100000); // 총 합계 210만 원
        assertThat(response.orderNumber()).startsWith("ORD-");

        // Then 2: DB 저장 상태 상세 검증 (JdbcTemplate 사용)

        // 2-1. Order 테이블 확인
        Map<String, Object> savedOrder = jdbcTemplate.queryForMap(
                "SELECT * FROM orders WHERE order_number = ?", response.orderNumber());
        assertThat(savedOrder.get("total_amount")).isEqualTo(2100000);

        // 2-2. OrderDelivery 테이블 확인
        Map<String, Object> savedDelivery = jdbcTemplate.queryForMap(
                "SELECT * FROM order_delivery WHERE order_id = ?", savedOrder.get("id"));
        assertThat(savedDelivery.get("delivery_address")).isEqualTo("서울시 강남구 테헤란로");

        // 2-3. OrderProducts 테이블 확인
        Integer productCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM order_products WHERE order_id = ?", Integer.class, savedOrder.get("id"));
        assertThat(productCount).isEqualTo(2);

        // 2-4. Payment 테이블 확인 (엔티티 생성 메서드 내에서 추가된 초기 결제 정보)
        Map<String, Object> savedPayment = jdbcTemplate.queryForMap(
                "SELECT * FROM payments WHERE order_id = ?", savedOrder.get("id"));

        // Enum 값 비교 시 DB 저장 형태(String)에 맞춰 대문자로 확인
        assertThat(savedPayment.get("status").toString()).isEqualTo("READY");
        assertThat(((Number) savedPayment.get("total_amount")).intValue()).isEqualTo(2100000);

        Integer stock101 = jdbcTemplate.queryForObject(
                "SELECT stock FROM products WHERE id = ?", Integer.class, 101L);
        Integer stock102 = jdbcTemplate.queryForObject(
                "SELECT stock FROM products WHERE id = ?", Integer.class, 102L);

        assertThat(stock101).isEqualTo(9);
        assertThat(stock102).isEqualTo(18);
    }
}
