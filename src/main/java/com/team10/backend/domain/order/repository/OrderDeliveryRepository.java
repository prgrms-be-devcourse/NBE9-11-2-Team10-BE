package com.team10.backend.domain.order.repository;

import com.team10.backend.domain.order.entity.OrderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery,Long> {
}
