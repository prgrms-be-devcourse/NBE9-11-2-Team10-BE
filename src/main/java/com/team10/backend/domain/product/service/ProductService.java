package com.team10.backend.domain.product.service;

import com.team10.backend.domain.product.dto.ProductCreateRequest;
import com.team10.backend.domain.product.dto.ProductDetailResponse;
import com.team10.backend.domain.product.dto.ProductInactiveResponse;
import com.team10.backend.domain.product.dto.ProductListResponse;
import com.team10.backend.domain.product.dto.ProductPageResponse;
import com.team10.backend.domain.product.dto.ProductStockRequest;
import com.team10.backend.domain.product.dto.ProductStockResponse;
import com.team10.backend.domain.product.dto.ProductUpdateRequest;
import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;
import com.team10.backend.domain.product.repository.ProductRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public ProductPageResponse list(int page, int size, ProductType type, ProductStatus status, Long sellerId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), ProductStatus.INACTIVE));
            }

            if (sellerId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), sellerId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);

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

    @Transactional(readOnly = true)
    public ProductDetailResponse detail(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return ProductDetailResponse.from(product);
    }

    @Transactional
    public ProductDetailResponse update(Long userId, Long productId, ProductUpdateRequest request) {

        Product product = getAuthorizedProduct(userId, productId);

        product.update(
                request.type(),
                request.productName(),
                request.description(),
                request.price(),
                request.imageUrl(),
                request.status()
        );

        return ProductDetailResponse.from(product);
    }

    @Transactional
    public ProductInactiveResponse inactive(Long userId, Long productId) {
        Product product = getAuthorizedProduct(userId, productId);

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_INACTIVE);
        }

        product.updateStatus(ProductStatus.INACTIVE);

        return ProductInactiveResponse.from(product);
    }

    @Transactional
    public ProductStockResponse updateStock(Long userId, Long productId, ProductStockRequest request) {
        Product product = getAuthorizedProduct(userId, productId);

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_INACTIVE);
        }

        product.updateStock(request.stock());

        return ProductStockResponse.of(product.getId(), product.getStock());
    }

    private Product getAuthorizedProduct(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return product;
    }
}