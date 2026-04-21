package com.team10.backend.domain.product.controller;

import com.team10.backend.domain.product.dto.ProductDetailResponse;
import com.team10.backend.domain.product.dto.ProductPageResponse;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;
import com.team10.backend.domain.product.service.ProductService;
import com.team10.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
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
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size,
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long sellerId
    ) {
        ProductPageResponse response = productService.list(page, size, type, status, sellerId);
        return ApiResponse.ok(response);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회", description = "상품 ID로 특정 상품의 상세 정보를 조회합니다.")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable Long productId) {
        return ApiResponse.ok(productService.detail(productId));
    }
}
