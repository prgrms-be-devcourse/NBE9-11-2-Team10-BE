package com.team10.backend.domain.product.service;

import com.team10.backend.domain.product.dto.ProductCreateRequest;
import com.team10.backend.domain.product.dto.ProductDetailResponse;
import com.team10.backend.domain.product.dto.ProductInactiveResponse;
import com.team10.backend.domain.product.dto.ProductListResponse;
import com.team10.backend.domain.product.dto.ProductPageResponse;
import com.team10.backend.domain.product.dto.ProductUpdateRequest;
import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;
import com.team10.backend.domain.product.repository.ProductRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductDetailResponse create(Long userId, ProductCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = new Product(
                user,
                request.type(),
                request.productName(),
                request.description(),
                request.price(),
                request.stock(),
                request.imageUrl()
        );

        Product savedProduct = productRepository.save(product);
        return ProductDetailResponse.from(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductPageResponse list(int page, int size, ProductType type, ProductStatus status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> productPage;

        if (type != null && status != null) productPage = productRepository.findByTypeAndStatus(type, status, pageable);
        else if (type != null) productPage = productRepository.findByType(type, pageable);
        else if (status != null) productPage = productRepository.findByStatus(status, pageable);
        else productPage = productRepository.findByStatusNot(ProductStatus.INACTIVE, pageable);

        List<ProductListResponse> content = productPage.getContent()
                .stream()
                .map(ProductListResponse::from)
                .toList();

        return new ProductPageResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    public ProductDetailResponse detail(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() == ProductStatus.INACTIVE) throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);

        return ProductDetailResponse.from(product);
    }

    @Transactional
    public ProductDetailResponse update(Long productId, ProductUpdateRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // TODO: 인증/인가 적용 후 판매자 권한 및 본인 상품 여부 검증 추가
        product.update(
                request.type(),
                request.productName(),
                request.description(),
                request.price(),
                request.stock(),
                request.imageUrl(),
                request.status()
        );

        return ProductDetailResponse.from(product);
    }

    @Transactional
    public ProductInactiveResponse inactive(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_INACTIVE);
        }

        product.updateStatus(ProductStatus.INACTIVE);

        return ProductInactiveResponse.from(product);
    }
}