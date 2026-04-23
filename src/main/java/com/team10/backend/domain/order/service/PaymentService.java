package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.confirm.ConfirmRequest;
import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.entity.Order;
import com.team10.backend.domain.order.entity.OrderDelivery;
import com.team10.backend.domain.order.entity.Payment;
import com.team10.backend.domain.order.repository.OrderDeliveryRepository;
import com.team10.backend.domain.order.repository.OrderRepository;
import com.team10.backend.domain.order.repository.PaymentRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.team10.backend.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final OrderConfirmService orderConfirmService; // 외부 API 호출 컴포넌트

    @Transactional
    public TossConfirmResponse confirmPayment(ConfirmRequest request) {
        //todo 유저 검증하는 부분, 해당 유저가 해당 오더 넘버를 가지고 있는게 맞는지
        // 1. 비즈니스 검증: DB의 금액과 요청 금액 비교 , 주문 생성시에
        log.info("조회 시도 - request.orderId: [{}]", request.orderId());
        Payment payment = paymentRepository.findByOrderNumber(request.orderId())
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        Order order = orderRepository.findByOrderNumber(request.orderId())
                .orElseThrow(() -> new BusinessException(ORDER_NOT_FOUND));

        OrderDelivery delivery = orderDeliveryRepository.findById(order.getId())
                .orElseThrow(() -> new BusinessException(DELIVERY_NOT_FOUND));

        //프론트에서 금액을 위조했을 경우
        if (payment.getTotalAmount()!=request.amount()) {
            throw new BusinessException(AMOUNT_MISMATCH);
        }

        // 2. 토스 서버로 승인 요청 (외부 통신)
        // paymentKey, orderId, amount를 헤더와 함께 전송
        TossConfirmResponse response = orderConfirmService.sendConfirmRequest(request,null);

        // 3. 결제 성공 후 상태 변경 (후처리)
        statusChangeAfterSuccess(payment,request.paymentKey(),order,delivery);

        return response;
    }

    public void statusChangeAfterSuccess(Payment payment, String paymentKey, Order order, OrderDelivery delivery) {
        completePayment(payment, paymentKey);
        updateOrderStatus(order);
        startReady(delivery);

         paymentRepository.save(payment);
         orderRepository.save(order);
        orderDeliveryRepository.save(delivery);
    }

    private void completePayment(Payment payment, String paymentKey) {
        log.info("결제 완료 처리 시작: {}", paymentKey);
        payment.completePayment(paymentKey);
    }

    private void updateOrderStatus(Order order) {
        order.successStatusOrder();
    }

    private void startReady(OrderDelivery delivery) {
        delivery.startReady();
    }
}