package com.team10.backend.domain.order.controller;

import com.team10.backend.domain.order.dto.OrderCreateRequest;
import com.team10.backend.domain.order.dto.search.OrderDetailResponse;
import com.team10.backend.domain.order.dto.search.buyer.OrderListResponse;
import com.team10.backend.domain.order.dto.OrderResponse;
import com.team10.backend.domain.order.dto.search.seller.SellerOrderListResponse;
import com.team10.backend.domain.order.service.OrderService;
import com.team10.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "주문", description = "주문 관련 API")
public class OrderController {

    private final OrderService orderService;

    //Todo 주문 검증

    @PostMapping
    @Operation(summary = "주문 등록", description = "구매자가 상품을 구매할때 주문을 생성합니다.")
    public ApiResponse<OrderResponse> createOrder(@RequestBody @Valid OrderCreateRequest req) {
        OrderResponse response = orderService.createOrder(req);
        return ApiResponse.ok(response);
    }

    @GetMapping("/buyer/{userId}")
    @Operation(summary = "구매자 주문 전체 조회",description = "유저가 본인 주문내역 전체를 확인할 수 있습니다.")
    public ApiResponse<OrderListResponse> getBuyerOrderList(@PathVariable("userId") Long userId) {
        OrderListResponse orderListResponse = orderService.getBuyderOrderList(userId);
        return ApiResponse.ok(orderListResponse);
    }

    @GetMapping("/seller/{userId}")
    @Operation(summary = "판매자 판매 내역 전체 조회",description = "판매자가 본인 판매내역 전체를 확인할 수 있습니다.")
    public ApiResponse<SellerOrderListResponse> getSellerOrderList(@PathVariable("userId") Long userId) {
        SellerOrderListResponse sellerOrderListResponse = orderService.getSellerOrderList(userId);
        return ApiResponse.ok(sellerOrderListResponse);
    }

    @GetMapping("/{userId}/{orderNumber}")
    @Operation(summary = "유저 주문 상세 내역 조회(판매자, 구매자)", description = "유저(판매자,구매자)가 주문 내역을 상세하게 확인할 수 있습니다.")
    public ApiResponse<OrderDetailResponse> getBuyerOrderDetail(@PathVariable("userId") Long userId,
                                                                @PathVariable("orderNumber") String orderNumber) {
        OrderDetailResponse orderDetailResponse = orderService.getBuyerOrderDetail(userId, orderNumber);
        return ApiResponse.ok(orderDetailResponse);
    }

}
