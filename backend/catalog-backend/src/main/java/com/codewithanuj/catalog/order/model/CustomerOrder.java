package com.codewithanuj.catalog.order.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An order placed through the storefront and handed off to WhatsApp.
 *
 * <p>There are no customer accounts, so the order carries its own contact details.
 * The {@code orderCode} (LC-1000, LC-1001, …) is the shared reference: it goes into
 * the WhatsApp message the customer sends, and it is what the owner searches for on the
 * admin Orders screen to match a chat to a record.
 *
 * <p>Named {@code CustomerOrder} rather than {@code Order} because {@code ORDER} is a
 * SQL keyword and {@code Order} is ambiguous in JPQL. The table is still {@code orders}.
 *
 * <p>The total is computed on the server from the item snapshots and never accepted
 * from the client — see {@code OrderService.placeOrder}.
 */
@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_address", nullable = false)
    private String customerAddress;

    @Column
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private String currency;

    /**
     * Eager because an order is never useful without its lines — every read of an order
     * renders them. {@code spring.jpa.open-in-view=false}, so a lazy collection would
     * throw the moment the DTO was built outside the transaction.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected CustomerOrder() {
    }

    public CustomerOrder(String orderCode, String customerName, String customerPhone,
                         String customerEmail, String customerAddress, String notes,
                         BigDecimal total, String currency) {
        this.orderCode = orderCode;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail;
        this.customerAddress = customerAddress;
        this.notes = notes;
        this.total = total;
        this.currency = currency;
    }

    /** Keeps both sides of the relationship consistent; the FK lives on the item. */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public UUID getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerAddress() { return customerAddress; }
    public String getNotes() { return notes; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public List<OrderItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
