package com.codewithanuj.catalog.order.controller;

import com.codewithanuj.catalog.order.dto.OrderResponseDto;
import com.codewithanuj.catalog.order.dto.OrderStatusUpdateRequest;
import com.codewithanuj.catalog.order.model.OrderStatus;
import com.codewithanuj.catalog.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Admin Orders screen: the list, one order, and moving its status along. */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** All orders newest first, or just one status when {@code status} is given. */
    @GetMapping
    public Page<OrderResponseDto> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return orderService.getOrders(status, pageable);
    }

    /** Orders still to be dealt with, for the admin summary badge. */
    @GetMapping("/new-count")
    public Map<String, Long> newCount() {
        return Map.of("count", orderService.countNew());
    }

    /** Looked up by the code the customer quotes in WhatsApp, not by the internal id. */
    @GetMapping("/{orderCode}")
    public OrderResponseDto getOrder(@PathVariable String orderCode) {
        return orderService.getByCode(orderCode);
    }

    @PatchMapping("/{orderCode}/status")
    public OrderResponseDto updateStatus(
            @PathVariable String orderCode,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(orderCode, request.status());
    }
}
