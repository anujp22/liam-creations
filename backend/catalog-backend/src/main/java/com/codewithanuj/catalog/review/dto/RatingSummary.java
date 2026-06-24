package com.codewithanuj.catalog.review.dto;

/**
 * Aggregate rating for a product (approved reviews only).
 * {@code average} is null when there are no approved reviews yet.
 */
public record RatingSummary(Double average, long count) {
}
