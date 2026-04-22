package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.webhook.WebhookPayload;
import com.team10.backend.domain.order.entity.IdempotencyRecord;
import com.team10.backend.domain.order.entity.Order;
import com.team10.backend.domain.order.entity.OrderDelivery;
import com.team10.backend.domain.order.entity.Payment;
import com.team10.backend.domain.order.enums.OrderStatus;
import com.team10.backend.domain.order.repository.IdempotencyRepository;
import com.team10.backend.domain.order.repository.OrderDeliveryRepository;
import com.team10.backend.domain.order.repository.OrderRepository;
import com.team10.backend.domain.order.repository.PaymentRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.team10.backend.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {

    private final OrderRepository orderRepository; // 주문 DB 내역
    private final PaymentRepository paymentRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final PaymentService paymentService;
    private final IdempotencyRepository idempotencyRepository;
    private final IdempotencyService idempotencyService;

    @Transactional
    public void processWebhook(WebhookPayload payload) {
        String orderId = payload.data().orderId();
        //웹훅에서 가져온 상태값 => 토스 서버
        String newStatus = payload.data().status(); // DONE,CANCELED,EXPIRED,ABORTED,

        // 1. DB에서 해당 주문 조회
        Order order = orderRepository.findByOrderNumber(orderId)
                .orElseThrow(() -> new BusinessException(ORDER_NOT_FOUND));

        IdempotencyRecord record = idempotencyRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(IDEMPOTENCY_NOT_FOUND));
        // 2. 멱등성 및 상태 체크
        // 이미 DB에서 완료(success)된 주문인데 웹훅이 또 왔다면 무시
        if (order.getStatus()== OrderStatus.SUCCESS && "DONE".equals(newStatus)) {
            log.info("이미 처리된 웹훅입니다. orderId={}", orderId);
            return;
        }

        // 3. DB가 SUCCESS가 아닌 상태 => 상태 업데이트 (주문 DB와 토스 DB 동기화)
        if ("DONE".equals(newStatus)) {
            // 토스 서버는 DONE인데, 우리 DB는 PENDING 상태
            // 결제 완료 처리 로직 (재고 감소)
            Payment payment = paymentRepository.findByOrderNumber(orderId)
                    .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
            OrderDelivery delivery = orderDeliveryRepository.findById(order.getId())
                    .orElseThrow(() -> new BusinessException(DELIVERY_NOT_FOUND));

            paymentService.statusChangeAfterSuccess(payment,payload.data().paymentKey(),order,delivery);
            idempotencyService.finalizeRecordFromWebhook(record, payload);
        } else if ("CANCELED".equals(newStatus)) {
            // 결제 취소 처리
        }

        log.info("주문 상태 업데이트 완료: orderId={}, status={}", orderId, newStatus);
    }
}