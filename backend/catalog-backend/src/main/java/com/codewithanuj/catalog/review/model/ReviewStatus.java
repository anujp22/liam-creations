package com.codewithanuj.catalog.review.model;

/** Moderation state of a customer review. New reviews start PENDING. */
public enum ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED
}
