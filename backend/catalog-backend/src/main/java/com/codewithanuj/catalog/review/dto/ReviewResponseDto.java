package com.codewithanuj.catalog.review.dto;

import com.codewithanuj.catalog.review.model.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

/** A review as returned by the API. Storefront sees only APPROVED ones. */
public record ReviewResponseDto(
        UUID id,
        String productNumber,
        int rating,
        String comment,
        ReviewStatus status,
        Instant createdAt
) {
}
