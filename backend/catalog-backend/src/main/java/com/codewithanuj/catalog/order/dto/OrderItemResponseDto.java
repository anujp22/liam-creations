package com.codewithanuj.catalog.order.dto;

import java.math.BigDecimal;

/** A stored order line. Prices here are the snapshot, not today's catalog price. */
public record OrderItemResponseDto(
        String productNumber,
        String title,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
