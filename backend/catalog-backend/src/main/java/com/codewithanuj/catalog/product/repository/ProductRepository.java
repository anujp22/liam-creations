package com.codewithanuj.catalog.product.repository;

import com.codewithanuj.catalog.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
