package com.team10.backend.domain.order.controller;

import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.global.exception.ErrorCode;
import com.team10.backend.global.security.CustomUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class OrderDeleteControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CustomUserPrincipal getMockUser(Long id, Role role) {
        return new CustomUserPrincipal(id, role);
    }
    // 2. Authentication 객체로 변환 (함수화)
    private UsernamePasswordAuthenticationToken getAuthentication(Long id, Role role) {
        CustomUserPrincipal principal = getMockUser(id, role);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    @Test
    @DisplayName("주문 삭제 성공 - 결제 대기 상태의 주문을 취소")
    void deleteOrder_Success() throws Exception {
        // 1. 유저 및 상품 세팅
        jdbcTemplate.update("INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role) VALUES (1, 'buyer@test.com', '1', '구매자', '구매자', '010', '서울', 'ACTIVE', 'BUYER')");
        jdbcTemplate.update("INSERT INTO products (id, user_id, product_name, price, stock, type, status) VALUES (101, 1, '상품', 10000, 9, 'BOOK', 'SELLING')");

        // 2. 주문 세팅 (READY 상태)
        String orderNum = "ORD-DELETE-001";
        jdbcTemplate.update("INSERT INTO orders (id, user_id, order_number, total_amount, status, is_deleted) VALUES (501, 1, ?, 10000, 'PENDING', false)", orderNum);
        jdbcTemplate.update("INSERT INTO order_products (id, order_id, product_id, quantity, order_price) VALUES (701, 501, 101, 1, 10000)");
        jdbcTemplate.update("INSERT INTO order_delivery (id, order_id, delivery_address, tracking_number, delivery_status) VALUES (801, 501, '주소', 'TRK-1', 'READY')");
        jdbcTemplate.update("INSERT INTO payments (id, order_id, order_number, total_amount, status) VALUES (901, 501, ?, 10000, 'READY')", orderNum);

        // When & Then
        mvc.perform(delete("/api/v1/orders/{userId}/{orderNumber}", 1L, orderNum))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value(orderNum))
                .andDo(print());

        // [참고] 환불/재고 로직이 추가되면 여기서 jdbcTemplate으로 재고가 늘어났는지 확인하는 로직을 추가할 예정
        Integer restoredStock = jdbcTemplate.queryForObject(
                "SELECT stock FROM products WHERE id = ?", Integer.class, 101L);

        assertEquals(Integer.valueOf(10), restoredStock);

        Map<String, Object> deletedOrder = jdbcTemplate.queryForMap(
                "SELECT * FROM orders WHERE order_number = ?", orderNum);

        assertEquals(Boolean.TRUE, deletedOrder.get("is_deleted"));
    }

    @Test
    @DisplayName("주문 삭제 실패 - 이미 배송 중인 상품 (SHIPPING)")
    void deleteOrder_Fail_AlreadyShipping() throws Exception {
        // 1. 데이터 세팅
        jdbcTemplate.update("INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role) VALUES (1, 'buyer@test.com', '1', '구매자', '구매자', '010', '서울', 'ACTIVE', 'BUYER')");
        String orderNum = "ORD-SHIPPING-001";
        jdbcTemplate.update("INSERT INTO orders (id, user_id, order_number, total_amount, status, is_deleted) VALUES (502, 1, ?, 10000, 'SUCCESS', false)", orderNum);

        // 배송 상태를 SHIPPING으로 설정
        jdbcTemplate.update("INSERT INTO order_delivery (id, order_id, delivery_address, tracking_number, delivery_status) VALUES (802, 502, '주소', 'TRK-2', 'SHIPPING')");
        jdbcTemplate.update("INSERT INTO payments (id, order_id, order_number, total_amount, status) VALUES (902, 502, ?, 10000, 'PAID')", orderNum);


        // When & Then
        mvc.perform(delete("/api/v1/orders/{userId}/{orderNumber}", 1L, orderNum))
                .andExpect(status().isBadRequest()) // CANNOT_CANCEL_SHIPPING_ORDER (400)
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CANNOT_CANCEL_SHIPPING_ORDER.getCode()))
                .andDo(print());
    }

    @Test
    @DisplayName("주문 삭제 실패 - 다른 유저의 주문을 삭제 시도")
    void deleteOrder_Fail_AccessDenied() throws Exception {
        // 1. 주인 유저(1)와 주문 생성
        jdbcTemplate.update("INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role) VALUES (1, 'owner@test.com', '1', '주인', '주인', '010', '서울', 'ACTIVE', 'BUYER')");
        String orderNum = "ORD-OTHER-001";
        jdbcTemplate.update("INSERT INTO orders (id, user_id, order_number, total_amount, status, is_deleted) VALUES (503, 1, ?, 10000, 'PENDING', false)", orderNum);
        jdbcTemplate.update("INSERT INTO order_delivery (id, order_id, delivery_status) VALUES (803, 503, 'READY')");
        jdbcTemplate.update("INSERT INTO payments (id, order_id, order_number, total_amount, status) VALUES (903, 503, ?, 10000, 'PAID')", orderNum);


        // 2. 공격자 유저(2) 생성
        jdbcTemplate.update("INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role) VALUES (2, 'hacker@test.com', '1', '해커', '해커', '010', '인천', 'ACTIVE', 'BUYER')");


        // When & Then: 유저 2가 유저 1의 주문 삭제 시도
        mvc.perform(delete("/api/v1/orders/{userId}/{orderNumber}", 2L, orderNum))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ACCESS_DENIED.getCode()))
                .andDo(print());
    }
}