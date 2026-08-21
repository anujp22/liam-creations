package com.codewithanuj.catalog.order.dto;

import com.codewithanuj.catalog.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** Admin: move an order to another status. */
public record OrderStatusUpdateRequest(
        @NotNull(message = "status is required")
        OrderStatus status
) {
}
