package com.codewithanuj.catalog.order.dto;

import com.codewithanuj.catalog.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An order as returned by the API.
 *
 * <p>The storefront uses the {@code orderCode}, {@code total} and {@code items} from
 * the create response to build the WhatsApp message, so that the number the customer
 * sends is by construction the number the owner sees on the admin screen — rather than
 * two independent calculations that agree most of the time.
 */
public record OrderResponseDto(
        UUID id,
        String orderCode,
        String customerName,
        String customerPhone,
        String customerEmail,
        String customerAddress,
        String notes,
        OrderStatus status,
        BigDecimal total,
        String currency,
        List<OrderItemResponseDto> items,
        Instant createdAt,
        Instant updatedAt
) {
}
