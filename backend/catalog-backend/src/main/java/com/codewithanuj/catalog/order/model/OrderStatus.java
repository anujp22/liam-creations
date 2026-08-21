package com.codewithanuj.catalog.order.model;

/**
 * Where an order has got to. Moved by hand by the shop owner on the admin Orders
 * screen — there are no payments or couriers wired in to move it automatically.
 *
 * <p>The usual path is {@code NEW → CONFIRMED → PAID → SHIPPED → DELIVERED}, with
 * {@code CANCELLED} available at any point. That order is a convention, not a rule the
 * server enforces; see {@code OrderService.updateStatus} for why.
 */
public enum OrderStatus {
    NEW,
    CONFIRMED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
