package com.codewithanuj.catalog.product.service;

import com.codewithanuj.catalog.product.dto.ProductCreateRequest;
import com.codewithanuj.catalog.product.dto.ProductResponseDto;
import com.codewithanuj.catalog.product.dto.UpdateFeaturedRequest;
import com.codewithanuj.catalog.product.dto.UpdateStatusRequest;
import com.codewithanuj.catalog.product.dto.ProductUpdateRequest;
import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductStatus;
import com.codewithanuj.catalog.product.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService() {
        this.productRepository = null;
    }

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponseDto> getAllProducts() {
        return getProducts().stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductResponseDto> getProductsByStatus(ProductStatus status) {
        return getProducts().stream()
                .filter(product -> product.getStatus() == status)
                .map(this::toDto)
                .toList();
    }

    public Optional<ProductResponseDto> getProductByProductNumber(String productNumber) {
        return getProducts().stream()
                .filter(product -> product.getProductNumber().equalsIgnoreCase(productNumber))
                .findFirst()
                .map(this::toDto);
    }

    public ProductResponseDto createProduct(ProductCreateRequest request) {
        if (productRepository.existsById(request.productNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product already exists: " + request.productNumber());
        }

        Product product = new Product(
                request.productNumber(),
                request.title(),
                request.description(),
                request.price(),
                request.currency(),
                request.status(),
                request.featured(),
                request.instagramPostUrl()
        );

        return toDto(productRepository.save(product));
    }

    public ProductResponseDto updateProduct(String productNumber, ProductUpdateRequest request) {
        if (!productRepository.existsById(productNumber)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productNumber);
        }

        Product updated = new Product(
                productNumber,
                request.title(),
                request.description(),
                request.price(),
                request.currency(),
                request.status(),
                request.featured(),
                request.instagramPostUrl()
        );

        return toDto(productRepository.save(updated));
    }

    public ProductResponseDto updateFeatured(String productNumber, UpdateFeaturedRequest request) {
        Product existing = productRepository.findById(productNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productNumber));

        Product updated = new Product(
                existing.getProductNumber(),
                existing.getTitle(),
                existing.getDescription(),
                existing.getPrice(),
                existing.getCurrency(),
                existing.getStatus(),
                request.featured(),
                existing.getInstagramPostUrl()
        );

        return toDto(productRepository.save(updated));
    }

    public ProductResponseDto updateStatus(String productNumber, UpdateStatusRequest request) {
        Product existing = productRepository.findById(productNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productNumber));

        Product updated = new Product(
                existing.getProductNumber(),
                existing.getTitle(),
                existing.getDescription(),
                existing.getPrice(),
                existing.getCurrency(),
                request.status(),
                existing.isFeatured(),
                existing.getInstagramPostUrl()
        );

        return toDto(productRepository.save(updated));
    }

    private List<Product> getProducts() {
        if (productRepository != null) {
            return productRepository.findAll();
        }

        return getMockProducts();
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
