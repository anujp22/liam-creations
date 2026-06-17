package com.codewithanuj.catalog.product.service;

import com.codewithanuj.catalog.product.dto.ProductCreateRequest;
import com.codewithanuj.catalog.product.dto.ProductResponseDto;
import com.codewithanuj.catalog.product.dto.ProductUpdateRequest;
import com.codewithanuj.catalog.product.dto.UpdateFeaturedRequest;
import com.codewithanuj.catalog.product.dto.UpdateStatusRequest;
import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductCategory;
import com.codewithanuj.catalog.product.model.ProductStatus;
import com.codewithanuj.catalog.product.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toDto);
    }

    public Page<ProductResponseDto> getProductsByStatus(ProductStatus status, Pageable pageable) {
        return productRepository.findByStatus(status, pageable).map(this::toDto);
    }

    public Page<ProductResponseDto> getProductsByCategory(ProductCategory category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable).map(this::toDto);
    }

    public Page<ProductResponseDto> getProductsByStatusAndCategory(ProductStatus status, ProductCategory category, Pageable pageable) {
        return productRepository.findByStatusAndCategory(status, category, pageable).map(this::toDto);
    }

    public Page<ProductResponseDto> getProducts(ProductStatus status, ProductCategory category, String search, Pageable pageable) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return productRepository.findFiltered(status, category, normalizedSearch, pageable).map(this::toDto);
    }

    public Optional<ProductResponseDto> getProductByProductNumber(String productNumber) {
        return productRepository.findById(productNumber).map(this::toDto);
    }

    @Transactional
    public void deleteProduct(String productNumber) {
        if (!productRepository.existsById(productNumber)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + productNumber);
        }
        productRepository.deleteById(productNumber);
    }

    @Transactional
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
                request.imageUrl(),
                request.category()
        );

        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductResponseDto updateProduct(String productNumber, ProductUpdateRequest request) {
        if (!productRepository.existsById(productNumber)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + productNumber);
        }

        Product updated = new Product(
                productNumber,
                request.title(),
                request.description(),
                request.price(),
                request.currency(),
                request.status(),
                request.featured(),
                request.imageUrl(),
                request.category()
        );

        return toDto(productRepository.save(updated));
    }

    @Transactional
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
                existing.getImageUrl(),
                existing.getCategory()
        );

        return toDto(productRepository.save(updated));
    }

    @Transactional
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
                existing.getImageUrl(),
                existing.getCategory()
        );

        return toDto(productRepository.save(updated));
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
                product.getImageUrl(),
                product.getCategory()
        );
    }
}
