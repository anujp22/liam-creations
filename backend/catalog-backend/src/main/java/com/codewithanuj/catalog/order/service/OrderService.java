package com.codewithanuj.catalog.order.service;

import com.codewithanuj.catalog.order.dto.OrderCreateRequest;
import com.codewithanuj.catalog.order.dto.OrderItemRequest;
import com.codewithanuj.catalog.order.dto.OrderItemResponseDto;
import com.codewithanuj.catalog.order.dto.OrderResponseDto;
import com.codewithanuj.catalog.order.model.CustomerOrder;
import com.codewithanuj.catalog.order.model.OrderItem;
import com.codewithanuj.catalog.order.model.OrderStatus;
import com.codewithanuj.catalog.order.repository.CustomerOrderRepository;
import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderCodeGenerator orderCodeGenerator;

    public OrderService(CustomerOrderRepository orderRepository,
                        ProductRepository productRepository,
                        OrderCodeGenerator orderCodeGenerator) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderCodeGenerator = orderCodeGenerator;
    }

    /**
     * Public submission. Prices every line from the database and computes the total
     * itself — the request carries product numbers and quantities only, so there is
     * nothing a caller could inflate or discount.
     *
     * <p>If any requested product has vanished or been soft-deleted since the cart was
     * loaded, the whole order is rejected with 409 and the offending product numbers.
     * Quietly dropping a line would hand the customer a smaller order than the one they
     * pressed the button on.
     */
    @Transactional
    public OrderResponseDto placeOrder(OrderCreateRequest request) {
        Map<String, Integer> quantities = mergeDuplicateLines(request.items());

        List<Product> found = productRepository.findAllById(quantities.keySet()).stream()
                .filter(product -> !product.isDeleted())
                .toList();

        List<String> unavailable = quantities.keySet().stream()
                .filter(number -> found.stream().noneMatch(p -> p.getProductNumber().equals(number)))
                .toList();
        if (!unavailable.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No longer available: " + String.join(", ", unavailable));
        }

        Map<String, Product> byNumber = found.stream()
                .collect(Collectors.toMap(Product::getProductNumber, product -> product));

        // The catalog is single-currency in practice, but nothing enforces that per row.
        // Summing across currencies would produce a total that means nothing, so say so
        // rather than invent a number.
        List<String> currencies = found.stream().map(Product::getCurrency).distinct().toList();
        if (currencies.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "These products are priced in different currencies (" + String.join(", ", currencies)
                            + ") and cannot be ordered together.");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> lines = new ArrayList<>();
        // Iterate the request's order, not the database's, so the stored lines read the
        // same way round as the cart the customer was looking at.
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            Product product = byNumber.get(entry.getKey());
            int quantity = entry.getValue();
            BigDecimal unitPrice = effectivePrice(product);
            lines.add(new OrderItem(product.getProductNumber(), product.getTitle(), unitPrice, quantity));
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        CustomerOrder order = new CustomerOrder(
                orderCodeGenerator.next(),
                request.customerName().trim(),
                request.customerPhone().trim(),
                blankToNull(request.customerEmail()),
                request.customerAddress().trim(),
                blankToNull(request.notes()),
                total,
                currencies.get(0)
        );
        lines.forEach(order::addItem);

        // saveAndFlush, not save: @CreationTimestamp is populated by the insert, and
        // building the DTO from an unflushed entity returns null timestamps. That is a
        // live bug in the product create response; no reason to reproduce it here.
        return toDto(orderRepository.saveAndFlush(order));
    }

    /** Admin: every order, or just those in one status. Newest first. */
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrders(OrderStatus status, Pageable pageable) {
        Page<CustomerOrder> page = (status == null)
                ? orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                : orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return page.map(this::toDto);
    }

    /** Admin: one order by the code the customer quoted. */
    @Transactional(readOnly = true)
    public OrderResponseDto getByCode(String orderCode) {
        return toDto(requireByCode(orderCode));
    }

    /**
     * Admin: move an order to another status.
     *
     * <p>Any status may follow any other, deliberately. The obvious alternative is to
     * enforce {@code NEW → CONFIRMED → PAID → SHIPPED → DELIVERED} and refuse to go
     * backwards, but this screen is driven by one person on a phone, and the realistic
     * mistake is a mis-tap, not a philosophically invalid transition. Under a forward-
     * only rule an accidental "Delivered" would be permanent. Letting the owner correct
     * their own typo is worth more than a workflow the server polices.
     */
    @Transactional
    public OrderResponseDto updateStatus(String orderCode, OrderStatus status) {
        CustomerOrder order = requireByCode(orderCode);
        order.setStatus(status);
        return toDto(orderRepository.save(order));
    }

    /** Count of orders not yet dealt with, for the admin summary badge. */
    @Transactional(readOnly = true)
    public long countNew() {
        return orderRepository.countByStatus(OrderStatus.NEW);
    }

    private CustomerOrder requireByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + orderCode));
    }

    /**
     * The same product listed twice becomes one line with the quantities added. The cart
     * cannot produce that, but this endpoint is public and a hand-built request can.
     * Without this the order would carry two lines for one product and the sum would
     * still be right, which is the kind of thing nobody notices until a customer asks.
     */
    private Map<String, Integer> mergeDuplicateLines(List<OrderItemRequest> items) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest item : items) {
            quantities.merge(item.productNumber(), item.quantity(), Integer::sum);
        }
        return quantities;
    }

    /** Mirrors the storefront's effectivePrice: the sale price wins when one is set. */
    private BigDecimal effectivePrice(Product product) {
        return product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private OrderResponseDto toDto(CustomerOrder order) {
        List<OrderItemResponseDto> items = order.getItems().stream()
                .map(item -> new OrderItemResponseDto(
                        item.getProductNumber(),
                        item.getTitle(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()))
                .toList();

        return new OrderResponseDto(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                order.getCustomerAddress(),
                order.getNotes(),
                order.getStatus(),
                order.getTotal(),
                order.getCurrency(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
