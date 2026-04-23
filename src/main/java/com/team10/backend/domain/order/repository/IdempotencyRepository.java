package com.team10.backend.domain.order.repository;

import com.team10.backend.domain.order.entity.IdempotencyRecord;
import com.team10.backend.domain.order.enums.IdempotencyStatus;
import com.team10.backend.domain.order.enums.RequestType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord,Long> {
    Optional<IdempotencyRecord> findByOrderId(String orderId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE IdempotencyRecord i SET i.status = 'PENDING', i.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE i.orderId = :orderId " +
            "AND i.type = :type " +
            "AND i.status != 'PENDING' " +
            "AND i.status != 'SUCCESS'")
    int updateStatusToPending(@Param("orderId") String orderId,@Param("type") RequestType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IdempotencyRecord i where i.orderId = :orderId and i.type = :type")
    Optional<IdempotencyRecord> findByOrderIdAndTypeForUpdate(
            @Param("orderId") String orderId,
            @Param("type") RequestType type
    );


    Optional<IdempotencyRecord> findByOrderIdAndType(String orderId, RequestType type);
}
