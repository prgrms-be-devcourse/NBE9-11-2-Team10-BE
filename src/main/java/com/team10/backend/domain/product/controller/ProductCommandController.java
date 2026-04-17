package com.team10.backend.domain.product.controller;

import com.team10.backend.domain.product.dto.ProductCreateRequest;
import com.team10.backend.domain.product.dto.ProductDetailResponse;
import com.team10.backend.domain.product.dto.ProductInactiveResponse;
import com.team10.backend.domain.product.dto.ProductUpdateRequest;
import com.team10.backend.domain.product.service.ProductService;
import com.team10.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores/me/products")
@Tag(name = "상품 관리", description = "상품 등록/수정/삭제 API")
public class ProductCommandController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "상품 등록", description = "판매자가 신규 상품을 등록합니다.")
    public ApiResponse<ProductDetailResponse> create(
            @RequestBody @Valid ProductCreateRequest request) {
        return ApiResponse.ok(productService.create(1L, request));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "상품 수정", description = "판매자가 등록한 상품 정보를 수정합니다.")
    public ApiResponse<ProductDetailResponse> update(
            @PathVariable Long productId, @RequestBody @Valid ProductUpdateRequest request
    ) {
        return ApiResponse.ok(productService.update(productId, request));
    }

    @PatchMapping("/{productId}/inactive")
    @Operation(summary = "상품 삭제(비활성화)", description = "판매자가 등록한 상품을 삭제합니다.")
    public ApiResponse<ProductInactiveResponse> inactive(@PathVariable Long productId) {
        return ApiResponse.ok(productService.inactive(productId));
    }
}
