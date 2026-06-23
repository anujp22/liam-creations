package com.codewithanuj.catalog.product.dto;

import com.codewithanuj.catalog.product.model.ProductCategory;
import com.codewithanuj.catalog.product.model.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

// productNumber is assigned automatically by the server (see ProductNumberGenerator).
public record ProductCreateRequest(
        @NotBlank
        String title,
        @NotBlank
        String description,
        @NotNull
        @DecimalMin(value = "0.01", message = "price must be greater than 0")
        BigDecimal price,
        @NotBlank
        String currency,
        @NotNull
        ProductStatus status,
        boolean featured,
        String imageUrl,
        @NotNull
        ProductCategory category,
        BigDecimal salePrice,
        List<String> images
) {
}
