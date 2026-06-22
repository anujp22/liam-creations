package com.codewithanuj.catalog.product.dto;

import java.util.Map;

/**
 * Inventory snapshot for the admin dashboard. Counts reflect active (non-deleted)
 * products, except {@code deleted} which counts soft-deleted rows.
 */
public record MetricsResponseDto(
        long totalActive,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory,
        long featured,
        long onSale,
        long deleted
) {
}
