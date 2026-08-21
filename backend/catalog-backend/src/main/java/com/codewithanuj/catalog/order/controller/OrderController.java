package com.codewithanuj.catalog.order.controller;

import com.codewithanuj.catalog.order.dto.OrderCreateRequest;
import com.codewithanuj.catalog.order.dto.OrderResponseDto;
import com.codewithanuj.catalog.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one public order endpoint. Unauthenticated, like review submission — there are no
 * customer accounts, so requiring a login would mean requiring one to buy anything.
 *
 * <p>It is the most abusable route in the app: open to the internet and it writes
 * personal data. {@code ApiRateLimitFilter} caps it at 5/min per client IP, and the
 * request DTO bounds every field it stores.
 *
 * <p>The response carries the order code and the server's own prices and total. The
 * storefront builds the WhatsApp message from that response rather than from its own
 * arithmetic, so the figure the customer sends is the figure the owner sees.
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto placeOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.placeOrder(request);
    }
}
