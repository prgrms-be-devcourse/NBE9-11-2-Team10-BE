package com.team10.backend.domain.product.service;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductType;
import com.team10.backend.domain.product.repository.ProductRepository;
import com.team10.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product create(User user, String productName, int price, int stock, ProductType type) {
        Product product = new Product(user, type, productName, price, stock);
        return productRepository.save(product);
    }
}