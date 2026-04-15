package com.team10.backend.domain.product.service;

import com.team10.backend.domain.product.dto.ProductCreateRequest;
import com.team10.backend.domain.product.dto.ProductResponse;
import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.repository.ProductRepository;
import com.team10.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(User user, ProductCreateRequest request) {
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
        return ProductResponse.from(savedProduct);
    }
}