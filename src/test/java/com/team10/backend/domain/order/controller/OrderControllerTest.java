package com.team10.backend.domain.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.order.dto.OrderCreateRequest;
import com.team10.backend.domain.order.dto.OrderResponse;
import com.team10.backend.domain.order.service.OrderService;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 테스트 실행 전 실제 DB(H2)에 기초 데이터를 밀어넣습니다.
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM order_products");
        jdbcTemplate.update("DELETE FROM order_delivery");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");

        // 유저 삽입 (ID: 1)
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (1, 'buyer@test.com', '1234', '홍길동', '길동이', '010-1234-5678', '서울시', 'ACTIVE', 'BUYER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        );

        // 상품들 삽입 (각각 가격이 다름)
        jdbcTemplate.update("INSERT INTO products (id, user_id, product_name, price, stock, type, status, created_at, updated_at) VALUES (?, 1, ?, ?, 10, 'BOOK', 'SELLING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", 101L, "상품A", 10000);
        jdbcTemplate.update("INSERT INTO products (id, user_id, product_name, price, stock, type, status, created_at, updated_at) VALUES (?, 1, ?, ?, 10, 'BOOK', 'SELLING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", 102L, "상품B", 20000);
        jdbcTemplate.update("INSERT INTO products (id, user_id, product_name, price, stock, type, status, created_at, updated_at) VALUES (?, 1, ?, ?, 10, 'BOOK', 'SELLING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", 103L, "상품C", 30000);
    }


    @Test
    @DisplayName("주문 생성 성공 - 로직을 통해 합산 금액 검증")
    void t1_2() throws Exception {
        // Given: 요청 데이터 준비
        // 상품101(1만)x2 + 상품102(2만)x2 + 상품103(3만)x2
        OrderCreateRequest.OrderProductReq item1 = new OrderCreateRequest.OrderProductReq(101L, 2);
        OrderCreateRequest.OrderProductReq item2 = new OrderCreateRequest.OrderProductReq(102L, 2);
        OrderCreateRequest.OrderProductReq item3 = new OrderCreateRequest.OrderProductReq(103L, 2);

        // 예상 총 금액: (10000*2) + (20000*2) + (30000*2) = 120,000원
        int expectedTotalAmount = 120000;

        OrderCreateRequest req = new OrderCreateRequest(1L, "서울특별시 강남구 테헤란로", List.of(item1, item2, item3));
        System.out.println(orderService.createOrder(req));
        // When: 호출 (실제 서비스의 createOrder가 실행됨)
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andDo(print());

        // Then: 검증
        // 이제 mockResponse가 아니라 실제 서비스가 계산해서 반환한 값이 검증됩니다.
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.totalAmount").value(expectedTotalAmount)) // 실제 계산 결과 검증
                .andExpect(jsonPath("$.data.orderNumber").exists());
    }

    @Test
    @DisplayName("주문 생성 실패 - 필수 입력값 누락 (Validation)")
    void t2() throws Exception {
        // Given: 주소가 비어있고 상품이 없는 잘못된 요청
        OrderCreateRequest req = new OrderCreateRequest(1L, "", List.of());

        // When & Then
        mvc.perform(
                        post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andDo(print())
                .andExpect(status().isBadRequest()); // @Valid에 의해 400 에러 발생
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 상품 (BusinessException)")
    void t3() throws Exception {
        OrderCreateRequest.OrderProductReq item = new OrderCreateRequest.OrderProductReq(999L, 1);
        OrderCreateRequest req = new OrderCreateRequest(1L, "주소", List.of(item));

        // [중요] when(...).thenThrow(...) 코드를 아예 삭제하세요!
        // 실제 서비스가 실행되면서 productRepository.findById(999L)를 호출하고,
        // 결과가 없으니 서비스 로직에 작성하신 BusinessException이 자동으로 터집니다.

        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andDo(print());

        // Then: 서비스가 던진 BusinessException을 ExceptionHandler가 ProblemDetail로 변환했는지 확인
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.detail").value("상품을 찾을 수 없습니다. ID: 999"));
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 사용자 (BusinessException)")
    void t4() throws Exception {
        // Given: DB에 없는 사용자 ID 999L로 요청
        Long invalidUserId = 999L;
        OrderCreateRequest.OrderProductReq item = new OrderCreateRequest.OrderProductReq(101L, 1);
        OrderCreateRequest req = new OrderCreateRequest(invalidUserId, "서울특별시 강남구", List.of(item));

        // when(...) 삭제!
        // 서비스의 findUser(req) 내부에서 userRepository.findById(999L)가
        // 빈 Optional을 반환하여 BusinessException(USER_NOT_FOUND)이 발생.
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.detail").value("해당 유저 정보를 찾을 수 없습니다."));
    }
}