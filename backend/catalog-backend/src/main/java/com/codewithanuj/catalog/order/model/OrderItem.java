package com.codewithanuj.catalog.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One line of an order, with the product's title and price <strong>frozen at order
 * time</strong>.
 *
 * <p>The snapshot is the whole point. Prices change, products get renamed, and the
 * admin screen can delete a product outright — none of which may retroactively alter
 * what a customer was quoted. This is the same reasoning that removed the price from
 * the cart (A11), applied to the permanent record instead of the temporary one.
 *
 * <p>{@code productNumber} is stored as a plain value with no foreign key, so deleting
 * a product cannot cascade into order history. It is a reference for the owner's
 * benefit, not a live link.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(name = "product_number", nullable = false)
    private String productNumber;

    @Column(nullable = false)
    private String title;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {
    }

    public OrderItem(String productNumber, String title, BigDecimal unitPrice, int quantity) {
        this.productNumber = productNumber;
        this.title = title;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    /** Derived, not stored — there is no second source of truth to drift from. */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public UUID getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    void setOrder(CustomerOrder order) { this.order = order; }
    public String getProductNumber() { return productNumber; }
    public String getTitle() { return title; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
