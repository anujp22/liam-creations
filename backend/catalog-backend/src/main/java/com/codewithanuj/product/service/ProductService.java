package com.codewithanuj.product.service;

import com.codewithanuj.product.dto.ProductResponseDto;
import com.codewithanuj.product.model.Product;
import com.codewithanuj.product.model.ProductStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    public List<ProductResponseDto> getAllProducts() {
        return getMockProducts().stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductResponseDto> getProductsByStatus(ProductStatus status) {
        return getMockProducts().stream()
                .filter(product -> product.getStatus() == status)
                .map(this::toDto)
                .toList();
    }

    public Optional<ProductResponseDto> getProductByProductNumber(String productNumber) {
        return getMockProducts().stream()
                .filter(product -> product.getProductNumber().equalsIgnoreCase(productNumber))
                .findFirst()
                .map(this::toDto);
    }

    private List<Product> getMockProducts() {
        return List.of(
                new Product(
                        "PRD-001",
                        "Handmade Clay Mug",
                        "A simple handmade mug for coffee or tea.",
                        new BigDecimal("24.99"),
                        "USD",
                        ProductStatus.IN_STOCK,
                        true,
                        "https://instagram.com/p/sample-product-1"
                ),
                new Product(
                        "PRD-002",
                        "Minimal Desk Lamp",
                        "A compact lamp with a clean modern look.",
                        new BigDecimal("49.99"),
                        "USD",
                        ProductStatus.OUT_OF_STOCK,
                        false,
                        "https://instagram.com/p/sample-product-2"
                ),
                new Product(
                        "PRD-003",
                        "Custom Wall Shelf",
                        "A made-to-order shelf for small home decor items.",
                        new BigDecimal("89.00"),
                        "USD",
                        ProductStatus.BUILT_ON_REQUEST,
                        true,
                        "https://instagram.com/p/sample-product-3"
                )
        );
    }

    private ProductResponseDto toDto(Product product) {
        return new ProductResponseDto(
                product.getProductNumber(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getStatus(),
                product.isFeatured(),
                product.getInstagramPostUrl()
        );
    }
}
