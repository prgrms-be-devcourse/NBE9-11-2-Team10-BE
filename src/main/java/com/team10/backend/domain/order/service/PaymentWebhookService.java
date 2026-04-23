package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.webhook.WebhookPayload;
import com.team10.backend.domain.order.entity.IdempotencyRecord;
import com.team10.backend.domain.order.entity.Order;
import com.team10.backend.domain.order.entity.OrderDelivery;
import com.team10.backend.domain.order.entity.Payment;
import com.team10.backend.domain.order.enums.IdempotencyStatus;
import com.team10.backend.domain.order.enums.OrderStatus;
import com.team10.backend.domain.order.enums.RequestType;
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
        // 1. 멱등성 레코드를 먼저 '비관적 락'으로 조회하여 선점
        // 재시도 로직이 돌고 있다면 여기서 대기하거나 순서가 정해집니다.
        RequestType type = "CANCELED".equals(newStatus) ? RequestType.CANCEL : RequestType.PAYMENT;
        IdempotencyRecord record = idempotencyRepository.findByOrderIdAndTypeForUpdate(orderId, type)
                .orElseThrow(() -> new BusinessException(IDEMPOTENCY_NOT_FOUND));

        // 이미 DB에서 완료(success)된 주문인데 웹훅이 또 왔다면 무시
        // 2. 최종 상태 체크 (이미 성공했거나 이미 취소된 경우 중복 처리 방지)
        if (record.getStatus() == IdempotencyStatus.SUCCESS && "DONE".equals(newStatus)) {
            log.info("이미 완료된 주문입니다. 중복 웹훅 무시: orderId={}", orderId);
            return;
        }
        // 1. DB에서 해당 주문 조회
        Order order = orderRepository.findByOrderNumber(orderId)
                .orElseThrow(() -> new BusinessException(ORDER_NOT_FOUND));


        // 3. DB가 SUCCESS가 아닌 상태 => 상태 업데이트 (주문 DB와 토스 DB 동기화)
        if ("DONE".equals(newStatus)) {
            // 토스 서버는 DONE인데, 우리 DB는 PENDING 상태
            // 결제 완료 처리 로직 (재고 감소)
            handlePaymentSuccess(order, payload, record);
        } else if ("CANCELED".equals(newStatus)) {
            // TODO: 결제 취소 로직 (주문 상태 변경 및 재고 복구 등)
            log.info("결제 취소 웹훅 처리 중: orderId={}", orderId);
            // 취소 관련 finalize 처리도 필요
            handlePaymentCancel(order, payload, record);
        }

        log.info("주문 상태 업데이트 완료: orderId={}, status={}", orderId, newStatus);
    }

    private void handlePaymentSuccess(Order order, WebhookPayload payload, IdempotencyRecord record) {
        Payment payment = paymentRepository.findByOrderNumber(order.getOrderNumber())
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
        OrderDelivery delivery = orderDeliveryRepository.findById(order.getId())
                .orElseThrow(() -> new BusinessException(DELIVERY_NOT_FOUND));

        // 서비스에서 상태 변경 + 재고 감소 등 수행
        paymentService.statusChangeAfterSuccess(payment, payload.data().paymentKey(), order, delivery);
        idempotencyService.finalizeRecordFromWebhook(record, payload);
    }
    private void handlePaymentCancel(Order order, WebhookPayload payload, IdempotencyRecord record) {
        log.info("결제 취소 웹훅 처리 중: orderId={}", order.getOrderNumber());
        // TODO: 취소에 따른 재고 복구 로직 등을 paymentService에 구현 후 호출
        // order.setStatus(OrderStatus.CANCELED);
        idempotencyService.finalizeRecordFromWebhook(record, payload);
    }
}