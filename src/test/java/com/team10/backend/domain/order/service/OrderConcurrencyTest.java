//package com.team10.backend.domain.order.service;
//
//import com.team10.backend.domain.order.dto.OrderCreateRequest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@ActiveProfiles("test")
//@Transactional(propagation = Propagation.NOT_SUPPORTED)
//public class OrderConcurrencyTest {
//
//    @DynamicPropertySource
//    static void overrideProps(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", () ->
//                "jdbc:h2:mem:order_concurrency_" + UUID.randomUUID()
//                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
//        registry.add("spring.datasource.username", () -> "sa");
//        registry.add("spring.datasource.password", () -> "");
//        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
//    }
//
//    @Autowired
//    private OrderService orderService;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    @Test
//    @DisplayName("동시 주문 - 재고가 1개 남은 상품은 2명 중 1명만 주문 성공")
//    void t2() throws Exception {
//        jdbcTemplate.update("DELETE FROM payments");
//        jdbcTemplate.update("DELETE FROM order_products");
//        jdbcTemplate.update("DELETE FROM order_delivery");
//        jdbcTemplate.update("DELETE FROM orders");
//        jdbcTemplate.update("DELETE FROM products");
//        jdbcTemplate.update("DELETE FROM users");
//
//        jdbcTemplate.update(
//                "INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
//                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
//                1L, "user1@test.com", "1234", "구매자1", "u1", "010-1111-1111", "서울", "ACTIVE", "BUYER"
//        );
//
//        jdbcTemplate.update(
//                "INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
//                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
//                2L, "user2@test.com", "1234", "구매자2", "u2", "010-2222-2222", "대전", "ACTIVE", "BUYER"
//        );
//
//        jdbcTemplate.update(
//                "INSERT INTO products (id, user_id, product_name, price, stock, type, status, created_at, updated_at) " +
//                        "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
//                201L, 1L, "자바스프링", 10000, 1, "BOOK", "SELLING"
//        );
//
//        // 201번 상품 1개 주문 데이터
//        OrderCreateRequest.OrderProductReq item = new OrderCreateRequest.OrderProductReq(201L, 1);
//
//        // 유저별 주문 요청
//        OrderCreateRequest req1 = new OrderCreateRequest(1L, "서울시", List.of(item));
//        OrderCreateRequest req2 = new OrderCreateRequest(2L, "대전시", List.of(item));
//
//        // 스레드 2개 생성
//        ExecutorService executorService = Executors.newFixedThreadPool(2);
//        // 동시 시작 제어
//        CountDownLatch readyLatch = new CountDownLatch(2);
//        CountDownLatch startLatch = new CountDownLatch(1);
//
//        // 성공/실패 횟수(기대값 성공 1, 실패 1)
//        AtomicInteger successCount = new AtomicInteger();
//        AtomicInteger failCount = new AtomicInteger();
//
//        // 동시 주문 작업을 각 스레드에 등록
//        Future<?> future1 = executorService.submit(() -> {
//            readyLatch.countDown();
//            try {
//                startLatch.await();
//                orderService.createOrder(req1);
//                successCount.incrementAndGet();
//            } catch (Exception e) {
//                failCount.incrementAndGet();
//            }
//        });
//
//        Future<?> future2 = executorService.submit(() -> {
//            readyLatch.countDown();
//            try {
//                startLatch.await();
//                orderService.createOrder(req2);
//                successCount.incrementAndGet();
//            } catch (Exception e) {
//                failCount.incrementAndGet();
//            }
//        });
//
//        // 두 스레드가 모두 시작 준비를 마칠 때까지 대기
//        readyLatch.await();
//        // 두 스레드에 동시에 시작 신호 전달
//        startLatch.countDown();
//
//        // 두 주문 작업이 모두 끝날 때까지 대기
//        future1.get();
//        future2.get();
//        // 스레드 종료
//        executorService.shutdown();
//
//        // 최종 재고 조회
//        Integer remainStock = jdbcTemplate.queryForObject(
//                "SELECT stock FROM products WHERE id = ?",
//                Integer.class,
//                201L
//        );
//
//        // 최종 주문수 조회
//        Integer orderCount = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM orders",
//                Integer.class
//        );
//
//        assertThat(successCount.get()).isEqualTo(1); // 1명 성공
//        assertThat(failCount.get()).isEqualTo(1);    // 1명 실패
//        assertThat(remainStock).isEqualTo(0);        // 재고수
//        assertThat(orderCount).isEqualTo(1);         // 주문수
//    }
//}
