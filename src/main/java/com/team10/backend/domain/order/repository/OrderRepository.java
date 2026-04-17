package com.team10.backend.domain.order.repository;

import com.team10.backend.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {

    // 유저 ID로 주문 목록을 찾되, 최신 주문이 위로 오도록 정렬
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    //orderNumber로 찾을 때 Fetch Join을 사용하면 쿼리 한 번으로 조회
    @Query("select o from Order o " +
            "join fetch o.delivery " +
            "join fetch o.orderProducts op " +
            "join fetch op.product " +
            "where o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithDetails(@Param("orderNumber") String orderNumber);
}
