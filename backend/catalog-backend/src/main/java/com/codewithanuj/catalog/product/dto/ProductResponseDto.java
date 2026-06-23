package com.codewithanuj.catalog.product.dto;

import com.codewithanuj.catalog.product.model.ProductCategory;
import com.codewithanuj.catalog.product.model.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponseDto(
        String productNumber,
        String title,
        String description,
        BigDecimal price,
        String currency,
        ProductStatus status,
        boolean featured,
        String imageUrl,
        ProductCategory category,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal salePrice,
        List<String> images
) {
}
