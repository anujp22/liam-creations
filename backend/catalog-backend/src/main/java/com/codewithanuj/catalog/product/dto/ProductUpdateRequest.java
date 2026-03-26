package com.codewithanuj.catalog.product.dto;

import com.codewithanuj.catalog.product.model.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^PRD-\\d{3}$", message = "productNumber must look like PRD-001")
        String productNumber,
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
        @NotBlank
        String instagramPostUrl
) {
}
