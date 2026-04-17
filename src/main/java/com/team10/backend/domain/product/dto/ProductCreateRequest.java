package com.team10.backend.domain.product.dto;

import com.team10.backend.domain.product.enums.ProductType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ProductCreateRequest(

        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 30, message = "상품명은 30자 이하여야 합니다.")
        String productName,

        String description,

        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        int price,

        @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        int stock,

        @URL(message = "올바른 이미지 URL 형식이어야 합니다.")
        String imageUrl,

        @NotNull(message = "상품 종류를 선택해 주세요.")
        ProductType type
) {
}