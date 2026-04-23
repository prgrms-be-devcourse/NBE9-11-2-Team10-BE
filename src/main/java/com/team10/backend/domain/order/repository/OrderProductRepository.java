package com.team10.backend.domain.order.repository;

import com.team10.backend.domain.order.entity.OrderProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderProductRepository extends JpaRepository<OrderProducts,Long> {

    //상품에 있는 sellerId를 가져와서 해당되는 orderId를 찾는다.
    @Query("SELECT op FROM OrderProducts op " +
            "JOIN FETCH op.order o " +
            "JOIN FETCH op.product p " +
            "WHERE p.user.id = :sellerId " +
            "ORDER BY o.createdAt DESC")
    List<OrderProducts> findAllBySellerId(@Param("sellerId") Long sellerId);
}
