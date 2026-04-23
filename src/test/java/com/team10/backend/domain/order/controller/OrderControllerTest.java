package com.team10.backend.domain.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.order.dto.OrderCreateRequest;
import com.team10.backend.domain.order.service.OrderService;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.global.exception.ErrorCode;
import com.team10.backend.global.security.CustomUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
        cleanupDatabase();

        /// 1. 기본 유저 세팅
        insertUser(1L, "buyer@test.com", "홍길동", "nickname1", "BUYER");   // 구매자
        insertUser(3L, "buyer3@test.com", "홍길동3", "nickname3", "BUYER");   // 주문 없는 구매자

        insertUser(2L, "seller@test.com", "홍길동2", "nickname2", "SELLER"); // 판매자
        insertUser(4L, "seller4@test.com", "홍길동4", "nickname4", "SELLER"); // 판매자 ,판매 상품 없음

        // 2. 기본 상품 세팅 (판매자 2L의 상품들)
        insertProduct(101L, 2L, "상품A", 10000);
        insertProduct(102L, 2L, "상품B", 20000);
        insertProduct(103L, 2L, "상품C", 30000);

        // 3. 주문 및 복합 상황 데이터 세팅
        setupDefaultOrders();
    }

    // ================= SQL 집중 관리 영역 =================

    private void cleanupDatabase() {
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM order_products");
        jdbcTemplate.update("DELETE FROM order_delivery");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");
    }

    private void insertUser(Long id, String email, String name, String nickname, String role) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (?, ?, '1234', ?, ?, '010-0000-0000', '주소', 'ACTIVE', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, email, name,nickname, role
        );
    }

    private void insertProduct(Long id, Long userId, String name, int price) {
        jdbcTemplate.update(
                "INSERT INTO products (id, user_id, product_name, price, stock, type, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 10, 'BOOK', 'SELLING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, userId, name, price
        );
    }

//    private void insertOrder(Long id, Long userId, String orderNum, int amount, String date) {
//        jdbcTemplate.update(
//                "INSERT INTO orders (id, user_id, order_number, total_amount, created_at, updated_at) " +
//                        "VALUES (?, ?, ?, ?, ?, ?)",
//                id, userId, orderNum, amount, date, date
//        );
//    }
    private void insertOrder(Long id, Long userId, String orderNum, int amount, String date) {
        jdbcTemplate.update(
                "INSERT INTO orders (id, user_id, order_number, total_amount, status, is_deleted, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                userId,
                orderNum,
                amount,
                "PENDING", // status 기본값 (Enum의 문자열 값)
                0,     // is_deleted 기본값
                date,
                date
        );
    }

    private void insertOrderProduct(Long id, Long orderId, Long productId, int qty, int price) {
        jdbcTemplate.update(
                "INSERT INTO order_products (id, order_id, product_id, quantity, order_price) VALUES (?, ?, ?, ?, ?)",
                id, orderId, productId, qty, price
        );
    }

    private void insertPayment(Long id, Long orderId, String orderNum, int amount, String status) {
        jdbcTemplate.update(
                "INSERT INTO payments (id, order_id, order_number, total_amount, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, orderId, orderNum, amount, status
        );
    }

    private void setupDefaultOrders() {
        // [기존 데이터] 과거 주문 (Buyer 1L이 상품A, B 구매)
        insertOrder(501L, 1L, "ORD-OLD-001", 30000, "2024-01-01 10:00:00");
        insertOrderProduct(1001L, 501L, 101L, 1, 10000); // ID: 1001L 추가
        insertOrderProduct(1002L, 501L, 102L, 1, 20000); // ID: 1002L 추가
        insertPayment(901L, 501L, "ORD-OLD-001", 30000, "PAID");

        // [기존 데이터] 최신 주문 (Buyer 1L이 상품C 구매)
        insertOrder(502L, 1L, "ORD-NEW-002", 30000, "2024-04-16 10:00:00");
        insertOrderProduct(1003L, 502L, 103L, 1, 30000); // ID: 1003L 추가
        insertPayment(902L, 502L, "ORD-NEW-002", 30000, "PAID");

        // -----------------------------------------------------------
        // 판매자 판매 내역 테스트용 데이터 (getSellerOrderList_Success용)
        // -----------------------------------------------------------

        // 1. 추가 유저: 제3의 판매자 (ID: 99L)
        insertUser(99L, "other@test.com", "남판매", "남다", "SELLER");

        // 2. 추가 상품: 제3의 판매자 상품 (ID: 202L)
        insertProduct(202L, 99L, "남 상품", 50000);

        // 3. 혼합 주문: ID 1L(홍길동)이 '내 상품(101L)'과 '남의 상품(202L)'을 동시에 주문
        String mixOrderNum = "ORD-MIX-001";
        insertOrder(601L, 1L, mixOrderNum, 60000, "2024-04-16 11:00:00");

        // 내 판매 내역 (상품A - 10000원) -> 판매자 ID 2L의 매출이 되어야 함
        insertOrderProduct(701L, 601L, 101L, 1, 10000);
        // 남의 판매 내역 (남 상품 - 50000원) -> 판매자 ID 99L의 매출
        insertOrderProduct(702L, 601L, 202L, 1, 50000);

        // 결제 정보
        insertPayment(801L, 601L, mixOrderNum, 60000, "PAID");
    }

    private CustomUserPrincipal getMockUser(Long id,  Role role) {
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
    @DisplayName("주문 생성 성공 - 로직을 통해 합산 금액 검증")
    void t1_2() throws Exception {
        // Given: 요청 데이터 준비
        // 상품101(1만)x2 + 상품102(2만)x2 + 상품103(3만)x2
        OrderCreateRequest.OrderProductReq item1 = new OrderCreateRequest.OrderProductReq(101L, 2);
        OrderCreateRequest.OrderProductReq item2 = new OrderCreateRequest.OrderProductReq(102L, 2);
        OrderCreateRequest.OrderProductReq item3 = new OrderCreateRequest.OrderProductReq(103L, 2);

        // 예상 총 금액: (10000*2) + (20000*2) + (30000*2) = 120,000원
        int expectedTotalAmount = 120000;

        OrderCreateRequest req = new OrderCreateRequest( "서울특별시 강남구 테헤란로", List.of(item1, item2, item3));
//        System.out.println(orderService.createOrder(req));
        // When: 호출 (실제 서비스의 createOrder가 실행됨)
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/orders")
                                .with(authentication(getAuthentication(1L, Role.BUYER)))
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
    @DisplayName("단일 상품 주문 생성 성공")
    void t1_3() throws Exception {
        // Given: 요청 데이터 준비
        // 상품101(1만)x2 + 상품102(2만)x2 + 상품103(3만)x2
        OrderCreateRequest.OrderProductReq item1 = new OrderCreateRequest.OrderProductReq(101L, 2);
        // 예상 총 금액: (10000*2)
        int expectedTotalAmount = 20000;

        OrderCreateRequest req = new OrderCreateRequest( "서울특별시 강남구 테헤란로", List.of(item1));
//        System.out.println(orderService.createOrder(req));
        // When: 호출 (실제 서비스의 createOrder가 실행됨)
        mvc.perform(post("/api/v1/orders")
                        .with(authentication(getAuthentication(1L, Role.BUYER))) // 함수 사용
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(20000))
                .andExpect(jsonPath("$.data.orderNumber").exists());

    }

    @Test
    @DisplayName("주문 생성 실패 - 필수 입력값 누락 (Validation)")
    void t2() throws Exception {
        // Given: 주소가 비어있고 상품이 없는 잘못된 요청
        OrderCreateRequest req = new OrderCreateRequest( "", List.of());

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
        OrderCreateRequest req = new OrderCreateRequest( "주소", List.of(item));

        // [중요] when(...).thenThrow(...) 코드를 아예 삭제하세요!
        // 실제 서비스가 실행되면서 productRepository.findById(999L)를 호출하고,
        // 결과가 없으니 서비스 로직에 작성하신 BusinessException이 자동으로 터집니다.

        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/orders")
                                .with(authentication(getAuthentication(1L, Role.BUYER)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andDo(print());

        // Then: 서비스가 던진 BusinessException을 ExceptionHandler가 ProblemDetail로 변환했는지 확인
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.detail").value("상품을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 사용자 (BusinessException)")
    void t4() throws Exception {
        // Given: DB에 없는 사용자 ID 999L로 요청
        Long invalidUserId = 999L;
        OrderCreateRequest.OrderProductReq item = new OrderCreateRequest.OrderProductReq(101L, 1);
        OrderCreateRequest req = new OrderCreateRequest( "서울특별시 강남구", List.of(item));

        // when(...) 삭제!
        // 서비스의 findUser(req) 내부에서 userRepository.findById(999L)가
        // 빈 Optional을 반환하여 BusinessException(USER_NOT_FOUND)이 발생.
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/orders")
                                .with(authentication(getAuthentication(invalidUserId, Role.BUYER)))
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
        mvc.perform(get("/api/v1/orders/buyer")
                        .with(authentication(getAuthentication(1L, Role.BUYER)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.userName").value("홍길동"))
                // 최신순 정렬 확인 (ID 502번 주문이 첫 번째에 와야 함)
                .andExpect(jsonPath("$.data.orders[0].orderNumber").value("ORD-MIX-001"))
                .andExpect(jsonPath("$.data.orders[0].representativeProductName").value("상품A 외 1건"))
                .andExpect(jsonPath("$.data.orders[0].totalQuantity").value(2))
                // 두 번째 주문 (외 N건 로직 확인)
                .andExpect(jsonPath("$.data.orders[1].orderNumber").value("ORD-NEW-002"))
                .andExpect(jsonPath("$.data.orders[1].representativeProductName").value("상품C"))
                .andExpect(jsonPath("$.data.orders[1].totalQuantity").value(1))
                .andExpect(jsonPath("$.data.orders[2].orderNumber").value("ORD-OLD-001"))
                .andExpect(jsonPath("$.data.orders[2].representativeProductName").value("상품A 외 1건"))
                .andExpect(jsonPath("$.data.orders[2].totalQuantity").value(2))
                .andDo(print());
    }

    @Test
    @DisplayName("구매자 주문 목록 조회 성공 - 주문 내역이 없는 경우")
    void t6() throws Exception {
        // When & Then
        mvc.perform(get("/api/v1/orders/buyer")
                        .with(authentication(getAuthentication(3L, Role.BUYER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders").isEmpty()) // 빈 리스트 반환 확인
                .andExpect(jsonPath("$.data.userName").value("홍길동3"));
    }

    @Test
    @DisplayName("구매자 주문 목록 조회 실패 - 존재하지 않는 유저")
    void getBuyerOrderList_UserNotFound() throws Exception {
        // Given: 존재하지 않는 ID (999)

        // When & Then
        // 서비스의 findUser(999)에서 예외가 발생하고, 이를 GlobalExceptionHandler가 잡는다고 가정
        mvc.perform(get("/api/v1/orders/buyer")
                        .with(authentication(getAuthentication(999L, Role.BUYER))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.detail").value("해당 유저 정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("구매자 주문 내역 조회 실패 - 구매자가 아닌 Seller ID로 조회 할 경우")
    void getOrderList_InvalidRole() throws Exception {


        // When & Then
        // 서비스 로직에서 Role 체크를 한다면 403이나 예외가 발생해야 함
        mvc.perform(get("/api/v1/orders/buyer")
                .with(authentication(getAuthentication(2L, Role.SELLER))))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.detail").value("해당 리소스에 대한 접근 권한이 없습니다."));
    }


    @Test
    @DisplayName("판매자 판매 내역 조회 성공 - 데이터 정합성 및 payment 상태 검증")
    void getSellerOrderList_Success() throws Exception {
        var auth = getAuthentication(2L, Role.SELLER);
        mvc.perform(get("/api/v1/orders/seller")
                        .with(authentication(auth))) // 판매자 2L로 조회
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sellerId").value(2L))
                // 검증 1: 판매자 2L의 상품은 '상품A, B, C'이며, 주문 내역 중 2L의 것만 필터링되어야 함
                // 위 setup 기준: 501L(상품A, B), 502L(상품C), 601L(상품A) 총 4개의 판매 행이 나옵니다.
                .andExpect(jsonPath("$.data.sales.length()").value(4))
                .andExpect(jsonPath("$.data.sales[?(@.productName == '상품A' && @.totalAmount == 10000)]").exists())
                .andExpect(jsonPath("$.data.sales[?(@.productName == '남 상품')]").doesNotExist()) // 남의 상품은 없어야 함
                .andDo(print());
    }

    @Test
    @DisplayName("판매자 판매 내역 조회 실패 - 판매자가 아닌 BUYER ID로 조회 할 경우")
    void getSellerOrderList_InvalidRole() throws Exception {
        // Given: BUYER 역할을 가진 유저 ID 1
        var auth = getAuthentication(1L, Role.BUYER);
        // When & Then
        // 서비스 로직에서 Role 체크를 한다면 403이나 예외가 발생해야 함
        mvc.perform(get("/api/v1/orders/seller").with(authentication(auth)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.detail").value("해당 리소스에 대한 접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("판매자 판매 내역 조회 성공 - 판매 내역이 전혀 없는 경우")
    void getSellerOrderList_Empty_Success() throws Exception {
        var auth = getAuthentication(4L, Role.SELLER);
        // When & Then
        mvc.perform(get("/api/v1/orders/seller").with(authentication(auth)))
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
        var auth = getAuthentication(9999L, Role.SELLER);
        // When & Then
        mvc.perform(get("/api/v1/orders/seller").with(authentication(auth)))
                .andExpect(status().isNotFound()) // GlobalExceptionHandler에서 404 처리 가정
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.detail").value("해당 유저 정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 수량 부족 (BusinessException)")
    void t1_4_fail_insufficient_stock() throws Exception {
        // Given: 상품 101L의 기본 재고는 10개로 세팅되어 있음 (setUp 참고)
        // 재고보다 많은 수량(11개)을 주문 요청
        OrderCreateRequest.OrderProductReq item = new OrderCreateRequest.OrderProductReq(101L, 11);
        OrderCreateRequest req = new OrderCreateRequest( "서울특별시 강남구", List.of(item));
        var auth = getAuthentication(1L, Role.BUYER);
        // When: 호출
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/orders")
                                .with(authentication(auth))
                                .contentType(MediaType.APPLICATION_JSON)

                                .content(objectMapper.writeValueAsString(req))
                )
                .andDo(print());

        // Then: 재고 부족 예외 및 에러 코드 검증
        resultActions
                .andExpect(status().isBadRequest()) // 비즈니스 예외 상황에 따른 상태코드 (보통 400 사용)
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.INSUFFICIENT_STOCK.getCode()))
                .andExpect(jsonPath("$.detail").value("재고가 부족합니다.")); // 메세지는 본인의 ErrorCode 설정에 맞게 수정
    }
}