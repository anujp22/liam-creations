package com.codewithanuj.product.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String productNumber;
    private String title;
    private String description;
    private BigDecimal price;
    private String currency;
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    private boolean featured;
    private String instagramPostUrl;

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
            String instagramPostUrl
    ) {
        this.productNumber = productNumber;
        this.title = title;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.status = status;
        this.featured = featured;
        this.instagramPostUrl = instagramPostUrl;
    }

    public String getProductNumber() {
        return productNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public String getInstagramPostUrl() {
        return instagramPostUrl;
    }
}
