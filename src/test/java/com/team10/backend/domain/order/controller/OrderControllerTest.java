package com.team10.backend.domain.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.order.dto.OrderCreateRequest;
import com.team10.backend.domain.order.dto.OrderResponse;
import com.team10.backend.domain.order.service.OrderService;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        // 3. 주문 데이터 생성 (과거 주문)
        String orderNum1 = "ORD-OLD-001";
        jdbcTemplate.update("INSERT INTO orders (id, user_id, order_number, total_amount, created_at, updated_at) VALUES (501, 1, ?, 30000, '2024-01-01 10:00:00', '2024-01-01 10:00:00')", orderNum1);
        jdbcTemplate.update("INSERT INTO order_products (order_id, product_id, quantity, order_price) VALUES (501, 101, 1, 10000)");
        jdbcTemplate.update("INSERT INTO order_products (order_id, product_id, quantity, order_price) VALUES (501, 102, 1, 20000)");
        jdbcTemplate.update("INSERT INTO payments (id, order_id, order_number, total_amount, status, created_at, updated_at) VALUES (901, 501, ?, 30000, 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", orderNum1);

        // 4. 주문 데이터 생성 (최신 주문)
        String orderNum2 = "ORD-NEW-002";
        jdbcTemplate.update("INSERT INTO orders (id, user_id, order_number, total_amount, created_at, updated_at) VALUES (502, 1, ?, 30000, '2024-04-16 10:00:00', '2024-04-16 10:00:00')", orderNum2);
        jdbcTemplate.update("INSERT INTO order_products (order_id, product_id, quantity, order_price) VALUES (502, 103, 1, 30000)");
        jdbcTemplate.update("INSERT INTO payments (id, order_id, order_number, total_amount, status, created_at, updated_at) VALUES (902, 502, ?, 30000, 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", orderNum2);
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

    @Test
    @DisplayName("구매자 주문 목록 조회 성공 - 최신순 정렬 및 대표명 검증")
    void t5() throws Exception {
        // When & Then
        mvc.perform(get("/api/v1/orders/buyer/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.userName").value("홍길동"))
                // 최신순 정렬 확인 (ID 502번 주문이 첫 번째에 와야 함)
                .andExpect(jsonPath("$.data.orders[0].orderNumber").value("ORD-NEW-002"))
                .andExpect(jsonPath("$.data.orders[0].representativeProductName").value("상품C"))
                .andExpect(jsonPath("$.data.orders[0].totalQuantity").value(1))
                // 두 번째 주문 (외 N건 로직 확인)
                .andExpect(jsonPath("$.data.orders[1].orderNumber").value("ORD-OLD-001"))
                .andExpect(jsonPath("$.data.orders[1].representativeProductName").value("상품A 외 1건"))
                .andExpect(jsonPath("$.data.orders[1].totalQuantity").value(2))
                .andDo(print());
    }

    @Test
    @DisplayName("구매자 주문 목록 조회 성공 - 주문 내역이 없는 경우")
    void t6() throws Exception {
        // Given: 주문이 없는 새로운 유저 추가
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (2, 'none@test.com', '1234', '이순신', '순신', '010-1234-5678', '서울시', 'ACTIVE', 'BUYER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        );
        // When & Then
        mvc.perform(get("/api/v1/orders/buyer/{userId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders").isEmpty()) // 빈 리스트 반환 확인
                .andExpect(jsonPath("$.data.userName").value("이순신"));
    }

    @Test
    @DisplayName("구매자 주문 목록 조회 실패 - 존재하지 않는 유저")
    void getBuyerOrderList_UserNotFound() throws Exception {
        // Given: 존재하지 않는 ID (999)

        // When & Then
        // 서비스의 findUser(999)에서 예외가 발생하고, 이를 GlobalExceptionHandler가 잡는다고 가정
        mvc.perform(get("/api/v1/orders/buyer/{userId}", 999L))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.detail").value("해당 유저 정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("판매자 판매 내역 조회 성공 - 데이터 정합성 및 payment 상태 검증")
    void getSellerOrderList_Success() throws Exception {
        // 1. 유저 생성 (판매자, 구매자, 타 판매자)
        jdbcTemplate.update("INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                "VALUES (10, 'seller@test.com', '1234', '이판매', '판다', '010-1-1', '서울', 'ACTIVE', 'SELLER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                "VALUES (20, 'unique_buyer@test.com', '1234', '김구매', '산다', '010-2-2', '부산', 'ACTIVE', 'BUYER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                "VALUES (99, 'other@test.com', '1234', '남판매', '남다', '010-9-9', '인천', 'ACTIVE', 'SELLER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        // 2. 상품 생성 (내 상품 201, 남의 상품 202)
        jdbcTemplate.update("INSERT INTO products (id, user_id, product_name, price, stock, type, status) VALUES (201, 10, '내 상품', 10000, 100, 'BOOK', 'SELLING')");
        jdbcTemplate.update("INSERT INTO products (id, user_id, product_name, price, stock, type, status) VALUES (202, 99, '남 상품', 50000, 100, 'BOOK', 'SELLING')");

        // 3. 상황: 한 주문(601)에 내 상품과 남의 상품이 섞여서 결제됨
        String orderNum = "ORD-MIX-001";
        jdbcTemplate.update("INSERT INTO orders (id, user_id, order_number, total_amount, created_at) VALUES (601, 20, ?, 60000, '2024-04-16 10:00:00')", orderNum);

        // 내 판매 내역 (10000원 * 1개)
        jdbcTemplate.update("INSERT INTO order_products (id, order_id, product_id, quantity, order_price) VALUES (701, 601, 201, 1, 10000)");
        // 남의 판매 내역 (50000원 * 1개)
        jdbcTemplate.update("INSERT INTO order_products (id, order_id, product_id, quantity, order_price) VALUES (702, 601, 202, 1, 50000)");

        // DTO에서 status를 가져오기 위한 Payment 데이터 (반드시 필요!)
        jdbcTemplate.update("INSERT INTO payments (id, order_id, order_number, total_amount, status) VALUES (801, 601, ?, 60000, 'PAID')", orderNum);

        // When & Then
        mvc.perform(get("/api/v1/orders/seller/{userId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sellerId").value(10L))
                // 검증 1: 내 판매 내역은 1건만 나와야 함 (남의 상품인 702번은 제외)
                .andExpect(jsonPath("$.data.sales.length()").value(1))
                // 검증 2: 판매된 상품 정보 확인
                .andExpect(jsonPath("$.data.sales[0].productName").value("내 상품"))
                .andExpect(jsonPath("$.data.sales[0].buyerName").value("김구매"))
                .andExpect(jsonPath("$.data.sales[0].totalAmount").value(10000)) // 10000 * 1
                .andExpect(jsonPath("$.data.sales[0].status").value("PAID"))
                .andDo(print());
    }

    @Test
    @DisplayName("판매자 판매 내역 조회 실패 - 판매자가 아닌 BUYER ID로 조회 할 경우")
    void getSellerOrderList_InvalidRole() throws Exception {
        // Given: BUYER 역할을 가진 유저 ID 1

        // When & Then
        // 서비스 로직에서 Role 체크를 한다면 403이나 예외가 발생해야 함
        mvc.perform(get("/api/v1/orders/seller/{userId}", 1L))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.detail").value("해당 리소스에 대한 접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("판매자 판매 내역 조회 성공 - 판매 내역이 전혀 없는 경우")
    void getSellerOrderList_Empty_Success() throws Exception {
        // 1. 판매자 유저 생성 (내역 없음)
        jdbcTemplate.update("INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                "VALUES (11, 'new_seller@test.com', '1234', '신입판', '새내기', '010-3-3', '광주', 'ACTIVE', 'SELLER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        // 2. 상품만 등록하고 주문은 없는 상태
        jdbcTemplate.update("INSERT INTO products (id, user_id, product_name, price, stock, type, status) VALUES (301, 11, '안 팔리는 상품', 1000, 10, 'BOOK', 'SELLING')");

        // When & Then
        mvc.perform(get("/api/v1/orders/seller/{userId}", 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sales").isArray())
                .andExpect(jsonPath("$.data.sales.length()").value(0)) // 빈 배열 확인
                .andDo(print());
    }

    @Test
    @DisplayName("판매자 판매 내역 조회 실패 - 존재하지 않는 유저 ID")
    void getSellerOrderList_UserNotFound() throws Exception {
        // Given: ID 9999는 DB에 없음

        // When & Then
        mvc.perform(get("/api/v1/orders/seller/{userId}", 9999L))
                .andExpect(status().isNotFound()) // GlobalExceptionHandler에서 404 처리 가정
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.detail").value("해당 유저 정보를 찾을 수 없습니다."));
    }
}