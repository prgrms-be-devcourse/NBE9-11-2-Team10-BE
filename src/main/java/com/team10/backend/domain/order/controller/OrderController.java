package com.team10.backend.domain.order.controller;

import com.team10.backend.domain.order.dto.OrderCreateRequest;
import com.team10.backend.domain.order.dto.OrderResponse;
import com.team10.backend.domain.order.service.OrderService;
import com.team10.backend.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@RequestBody @Valid OrderCreateRequest req) {
        OrderResponse response = orderService.createOrder(req);
        return ApiResponse.ok(response);
    }

}
