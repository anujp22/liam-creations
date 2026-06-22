package com.codewithanuj.catalog.product.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String productNumber;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String currency;
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    private boolean featured;
    private String imageUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;
    @Column(nullable = false)
    private boolean deleted = false;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;

    protected Product() {
    }

    public Product(
            String productNumber,
            String title,
            String description,
            BigDecimal price,
            String currency,
            ProductStatus status,
            boolean featured,
            String imageUrl,
            ProductCategory category
    ) {
        this.productNumber = productNumber;
        this.title = title;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.status = status;
        this.featured = featured;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public String getProductNumber() { return productNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getCurrency() { return currency; }
    public ProductStatus getStatus() { return status; }
    public boolean isFeatured() { return featured; }
    public String getImageUrl() { return imageUrl; }
    public ProductCategory getCategory() { return category; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
