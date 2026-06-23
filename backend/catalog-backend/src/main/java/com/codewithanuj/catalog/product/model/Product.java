package com.codewithanuj.catalog.product.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    // Lazy + batched: EAGER on a collection forces in-memory pagination of the
    // parent query (HHH000104). @BatchSize loads images for up to 30 products in
    // one extra query, avoiding N+1 across a page.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_number"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url", length = 1000)
    @BatchSize(size = 30)
    private List<String> images = new ArrayList<>();
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
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = (images == null) ? new ArrayList<>() : images; }
    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
