package com.codewithanuj.product.model;

import java.math.BigDecimal;

public class Product {

    private final String productNumber;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final String currency;
    private final ProductStatus status;
    private final boolean featured;
    private final String instagramPostUrl;

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
