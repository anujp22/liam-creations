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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductNumberGenerator productNumberGenerator;
    private final com.codewithanuj.catalog.shared.storage.StorageService storageService;

    public ProductService(ProductRepository productRepository,
                          ProductNumberGenerator productNumberGenerator,
                          com.codewithanuj.catalog.shared.storage.StorageService storageService) {
        this.productRepository = productRepository;
        this.productNumberGenerator = productNumberGenerator;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(ProductStatus status, ProductCategory category, String search, boolean onSale, Pageable pageable) {
        // One query handles every combination. The previous version returned early for
        // onSale and dropped status/category/search on the floor, so "sale + category"
        // silently returned the whole sale list.
        // Empty string, not null: an untyped null parameter makes Postgres reject
        // LOWER(?) at runtime. See the note on findFiltered.
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : "";
        return productRepository.findFiltered(status, category, normalizedSearch, onSale, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getDeletedProducts(Pageable pageable) {
        return productRepository.findByDeletedTrue(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponseDto> getProductByProductNumber(String productNumber) {
        return productRepository.findByProductNumberAndDeletedFalse(productNumber).map(this::toDto);
    }

    public com.codewithanuj.catalog.product.dto.MetricsResponseDto getMetrics() {
        java.util.Map<String, Long> byStatus = new java.util.LinkedHashMap<>();
        for (ProductStatus status : ProductStatus.values()) {
            byStatus.put(status.name(), productRepository.countByStatusAndDeletedFalse(status));
        }
        java.util.Map<String, Long> byCategory = new java.util.LinkedHashMap<>();
        for (ProductCategory category : ProductCategory.values()) {
            byCategory.put(category.name(), productRepository.countByCategoryAndDeletedFalse(category));
        }
        return new com.codewithanuj.catalog.product.dto.MetricsResponseDto(
                productRepository.countByDeletedFalse(),
                byStatus,
                byCategory,
                productRepository.countByFeaturedTrueAndDeletedFalse(),
                productRepository.countBySalePriceIsNotNullAndDeletedFalse(),
                productRepository.countByDeletedTrue()
        );
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
        Product product = productRepository.findById(productNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productNumber));

        // Gather every stored image (gallery + primary thumbnail) before the row goes.
        java.util.Set<String> urls = referencedUrls(product);

        productRepository.delete(product);

        deleteQuietly(urls);
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
        applyImages(product, request.images());

        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductResponseDto updateProduct(String productNumber, ProductUpdateRequest request) {
        Product product = productRepository.findById(productNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productNumber));
        validateSalePrice(request.price(), request.salePrice());

        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency());
        product.setStatus(request.status());
        product.setFeatured(request.featured());
        product.setCategory(request.category());
        product.setSalePrice(request.salePrice());

        // Snapshot what the product referenced before the update, so files that are no
        // longer referenced afterwards can be removed. Without this every photo the owner
        // removes from a product leaks its file forever.
        java.util.Set<String> before = referencedUrls(product);

        product.setImageUrl(request.imageUrl());
        applyImages(product, request.images());

        ProductResponseDto saved = toDto(productRepository.save(product));

        // Only delete what is genuinely unreferenced now — a URL kept in the new gallery,
        // or still used as the primary imageUrl, must survive.
        before.removeAll(referencedUrls(product));
        deleteQuietly(before);

        return saved;
    }

    @Transactional
    public ProductResponseDto updateFeatured(String productNumber, UpdateFeaturedRequest request) {
        Product product = productRepository.findById(productNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productNumber));
        product.setFeatured(request.featured());
        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductResponseDto updateStatus(String productNumber, UpdateStatusRequest request) {
        Product product = productRepository.findById(productNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productNumber));
        product.setStatus(request.status());
        return toDto(productRepository.save(product));
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
                product.getSalePrice(),
                // Copy into a detached list: forces the lazy collection to load
                // while the session is open, so JSON serialization can't trip a
                // LazyInitializationException (open-in-view is disabled).
                new java.util.ArrayList<>(product.getImages())
        );
    }

    /** Every stored file this product currently points at: gallery images plus the thumbnail. */
    private java.util.Set<String> referencedUrls(Product product) {
        java.util.Set<String> urls = new java.util.LinkedHashSet<>(product.getImages());
        if (product.getImageUrl() != null) {
            urls.add(product.getImageUrl());
        }
        return urls;
    }

    /** Best-effort file cleanup: storage problems must never fail the surrounding write. */
    private void deleteQuietly(java.util.Collection<String> urls) {
        for (String url : urls) {
            try {
                storageService.delete(url);
            } catch (RuntimeException ex) {
                log.warn("Could not delete unreferenced image {}: {}", url, ex.toString());
            }
        }
    }

    /** Stores the gallery and keeps the primary imageUrl in sync with the first image. */
    private void applyImages(Product product, java.util.List<String> images) {
        java.util.List<String> list = (images == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(images);
        product.setImages(list);
        if (!list.isEmpty()) {
            product.setImageUrl(list.get(0));
        }
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
