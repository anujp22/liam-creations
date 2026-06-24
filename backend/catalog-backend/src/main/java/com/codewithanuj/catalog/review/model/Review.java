package com.codewithanuj.catalog.review.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer review for a product: a 1–5 star rating and a comment. No login or
 * personal data is collected. Reviews are submitted as {@link ReviewStatus#PENDING}
 * and only become visible on the storefront once an admin approves them.
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "product_number", nullable = false)
    private String productNumber;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Review() {
    }

    public Review(String productNumber, int rating, String comment) {
        this.productNumber = productNumber;
        this.rating = rating;
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public String getProductNumber() { return productNumber; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
