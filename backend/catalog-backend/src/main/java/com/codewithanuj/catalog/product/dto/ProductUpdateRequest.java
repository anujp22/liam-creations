package com.codewithanuj.catalog.product.dto;

import com.codewithanuj.catalog.product.model.ProductStatus;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String productNumber,
        String title,
        String description,
        BigDecimal price,
        String currency,
        ProductStatus status,
        boolean featured,
        String instagramPostUrl
) {
}
