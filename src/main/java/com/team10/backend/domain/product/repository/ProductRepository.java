package com.team10.backend.domain.product.repository;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByType(ProductType type, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusNot(ProductStatus status, Pageable pageable);

    Page<Product> findByTypeAndStatus(ProductType type, ProductStatus status, Pageable pageable);
}
