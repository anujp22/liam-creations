package com.codewithanuj.product.repository;

import com.codewithanuj.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
