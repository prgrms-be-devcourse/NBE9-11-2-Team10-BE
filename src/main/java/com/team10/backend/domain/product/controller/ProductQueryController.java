package com.team10.backend.domain.product.controller;

import com.team10.backend.domain.product.dto.ProductPageResponse;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;
import com.team10.backend.domain.product.service.ProductService;
import com.team10.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@Tag(name = "상품 조회", description = "상품 조회 API")
public class ProductQueryController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "상품 전체 조회",
            description = "등록된 상품 목록을 최신순으로 조회하며, 페이지 및 필터 조건을 지원합니다.")
    public ApiResponse<ProductPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) ProductStatus status
    ) {
        ProductPageResponse response = productService.list(page, size, type, status);
        return ApiResponse.ok(response);
    }
}
