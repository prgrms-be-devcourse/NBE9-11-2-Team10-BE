package com.team10.backend.domain.order.repository;

import com.team10.backend.domain.order.entity.Order;
import com.team10.backend.domain.order.entity.Payment;
import com.team10.backend.domain.order.enums.RequestType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findByOrderNumber(String s);

    // 1. 해당 주문의 가장 최신 시도 기록 조회
    Optional<Payment> findFirstByOrderOrderByCreatedAtDesc(Order orderId);

    // 2. 비관적 락을 위한 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    List<Payment> findAllByOrderOrderByCreatedAtAsc(Order order);

    Optional<Payment> findByOrderNumberAndType(String orderNumber, RequestType type);
}
