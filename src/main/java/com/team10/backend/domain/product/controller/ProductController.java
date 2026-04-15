package com.team10.backend.domain.product.controller;

import com.team10.backend.domain.product.dto.ProductCreateRequest;
import com.team10.backend.domain.product.dto.ProductResponse;
import com.team10.backend.domain.product.service.ProductService;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.dto.ApiResponse;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores/me/products")
@Tag(name = "상품", description = "상품 관련 API")
public class ProductController {

    private final UserRepository userRepository;
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "상품 등록", description = "판매자가 신규 상품을 등록합니다.")
    public ApiResponse<ProductResponse> create(@RequestBody ProductCreateRequest request) {
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ProductResponse response = productService.create(user, request);
        return ApiResponse.ok(response);
    }
}