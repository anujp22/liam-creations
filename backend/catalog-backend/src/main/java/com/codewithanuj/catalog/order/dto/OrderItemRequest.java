package com.codewithanuj.catalog.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** One requested line: which product, and how many. The price is not the client's to say. */
public record OrderItemRequest(
        @NotBlank(message = "productNumber is required")
        String productNumber,

        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 99, message = "quantity must be at most 99")
        int quantity
) {
}
