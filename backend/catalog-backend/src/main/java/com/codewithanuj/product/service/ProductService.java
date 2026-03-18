package com.codewithanuj.product.service;

import com.codewithanuj.product.dto.ProductResponseDto;
import com.codewithanuj.product.model.Product;
import com.codewithanuj.product.model.ProductStatus;
import com.codewithanuj.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductResponseDto> getProductsByStatus(ProductStatus status) {
        return productRepository.findAll().stream()
                .filter(product -> product.getStatus() == status)
                .map(this::toDto)
                .toList();
    }

    public Optional<ProductResponseDto> getProductByProductNumber(String productNumber) {
        return productRepository.findById(productNumber)
                .map(this::toDto);
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
