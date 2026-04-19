package com.team10.backend.domain.order.service;

import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefundService {

    /**
     * 외부 결제사 환불 시뮬레이션
     */
    public void requestRefund(String paymentKey, int amount) {
        // 1. 여기서 실제 API 호출 대신 '성공/실패' 시뮬레이션한다.
        // 나중에 여기에 RestTemplate 로직이 들어간다.
        boolean isApiSuccess = simulateExternalApi(paymentKey);

        if (!isApiSuccess) {
            // 외부 결제사에서 거부된 상황을 가정 (예: 이미 취소됨, 잔액 부족)
//            throw new BusinessException(REFUND_API_FAILED, "외부 결제 시스템에서 환불이 거절되었습니다.");
        }

        // 성공 시 로그 기록
        System.out.println("LOG: 외부 환불 승인 완료 - Key: " + paymentKey + ", Amount: " + amount);
    }

    private boolean simulateExternalApi(String paymentKey) {
        // 테스트를 위해 paymentKey가 "fail"로 시작하면 실패하도록 설정 가능
        if (paymentKey != null && paymentKey.startsWith("fail")) return false;
        return true;
    }
//    비즈니스 예외이미 환불된 주문, 취소 가능 금액 초과예외 던짐 (롤백): 사용자에게 실패 알림인증/설정 예외API 키 만료, 잘못된 URL예외 던짐 (롤백): 관리자 알림 필요네트워크 예외결제사 서버 점검, 타임아웃재시도 또는 롤백: 가장 까다로운 부분
}