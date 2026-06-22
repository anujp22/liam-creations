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
    private final ProductNumberGenerator productNumberGenerator;

    public ProductService(ProductRepository productRepository, ProductNumberGenerator productNumberGenerator) {
        this.productRepository = productRepository;
        this.productNumberGenerator = productNumberGenerator;
    }

    public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
        return productRepository.findByDeletedFalse(pageable).map(this::toDto);
    }

    public Page<ProductResponseDto> getProductsByStatus(ProductStatus status, Pageable pageable) {
        return productRepository.findByStatusAndDeletedFalse(status, pageable).map(this::toDto);
    }

    public Page<ProductResponseDto> getProductsByCategory(ProductCategory category, Pageable pageable) {
        return productRepository.findByCategoryAndDeletedFalse(category, pageable).map(this::toDto);
    }

    public Page<ProductResponseDto> getProductsByStatusAndCategory(ProductStatus status, ProductCategory category, Pageable pageable) {
        return productRepository.findByStatusAndCategoryAndDeletedFalse(status, category, pageable).map(this::toDto);
    }

    public Page<ProductResponseDto> getProducts(ProductStatus status, ProductCategory category, String search, boolean onSale, Pageable pageable) {
        if (onSale) {
            return productRepository.findBySalePriceIsNotNullAndDeletedFalse(pageable).map(this::toDto);
        }
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        if (normalizedSearch != null) {
            return productRepository.findFiltered(status, category, normalizedSearch, pageable).map(this::toDto);
        }
        if (status != null && category != null) {
            return productRepository.findByStatusAndCategoryAndDeletedFalse(status, category, pageable).map(this::toDto);
        }
        if (status != null) {
            return productRepository.findByStatusAndDeletedFalse(status, pageable).map(this::toDto);
        }
        if (category != null) {
            return productRepository.findByCategoryAndDeletedFalse(category, pageable).map(this::toDto);
        }
        return productRepository.findByDeletedFalse(pageable).map(this::toDto);
    }

    public Page<ProductResponseDto> getDeletedProducts(Pageable pageable) {
        return productRepository.findByDeletedTrue(pageable).map(this::toDto);
    }

    public Optional<ProductResponseDto> getProductByProductNumber(String productNumber) {
        return productRepository.findById(productNumber).map(this::toDto);
    }

    /** Soft delete — hides the product but keeps the row (and its reserved number). */
    @Transactional
    public void deleteProduct(String productNumber) {
        Product product = productRepository.findById(productNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productNumber));
        product.setDeleted(true);
        productRepository.save(product);
    }

    /** Brings a soft-deleted product back into the active catalog. */
    @Transactional
    public ProductResponseDto restoreProduct(String productNumber) {
        Product product = productRepository.findById(productNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productNumber));
        product.setDeleted(false);
        return toDto(productRepository.save(product));
    }

    /** Permanently removes the row. The product number stays reserved by the sequence. */
    @Transactional
    public void permanentlyDeleteProduct(String productNumber) {
        if (!productRepository.existsById(productNumber)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + productNumber);
        }
        productRepository.deleteById(productNumber);
    }

    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequest request) {
        validateSalePrice(request.price(), request.salePrice());

        Product product = new Product(
                productNumberGenerator.next(),
                request.title(),
                request.description(),
                request.price(),
                request.currency(),
                request.status(),
                request.featured(),
                request.imageUrl(),
                request.category()
        );
        product.setSalePrice(request.salePrice());

        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductResponseDto updateProduct(String productNumber, ProductUpdateRequest request) {
        if (!productRepository.existsById(productNumber)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + productNumber);
        }
        validateSalePrice(request.price(), request.salePrice());

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
        updated.setSalePrice(request.salePrice());

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
        updated.setSalePrice(existing.getSalePrice());
        updated.setDeleted(existing.isDeleted());

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
        updated.setSalePrice(existing.getSalePrice());
        updated.setDeleted(existing.isDeleted());

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
                product.getCategory(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getSalePrice()
        );
    }

    private void validateSalePrice(java.math.BigDecimal price, java.math.BigDecimal salePrice) {
        if (salePrice == null) {
            return;
        }
        if (salePrice.signum() <= 0 || salePrice.compareTo(price) >= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "salePrice must be greater than 0 and less than price");
        }
    }
}
