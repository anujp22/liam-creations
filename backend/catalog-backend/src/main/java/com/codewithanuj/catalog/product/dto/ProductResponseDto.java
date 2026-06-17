package com.codewithanuj.catalog.product.dto;

import com.codewithanuj.catalog.product.model.ProductStatus;

import java.math.BigDecimal;

public record ProductResponseDto(
        String productNumber,
        String title,
        String description,
        BigDecimal price,
        String currency,
        ProductStatus status,
        boolean featured
) {
}
