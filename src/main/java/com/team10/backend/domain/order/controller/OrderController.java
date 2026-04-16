package com.team10.backend.domain.order.controller;

import com.team10.backend.domain.order.dto.OrderCreateRequest;
import com.team10.backend.domain.order.dto.OrderResponse;
import com.team10.backend.domain.order.service.OrderService;
import com.team10.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "주문", description = "주문 관련 API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "주문 등록", description = "구매자가 상품을 구매할때 주문을 생성합니다.")
    public ApiResponse<OrderResponse> createOrder(@RequestBody @Valid OrderCreateRequest req) {
        OrderResponse response = orderService.createOrder(req);
        return ApiResponse.ok(response);
    }

}
