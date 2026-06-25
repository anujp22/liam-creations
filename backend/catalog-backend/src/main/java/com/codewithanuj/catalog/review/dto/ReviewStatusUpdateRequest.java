package com.codewithanuj.catalog.review.dto;

import com.codewithanuj.catalog.review.model.ReviewStatus;
import jakarta.validation.constraints.NotNull;

/** Admin moderation: move a review to APPROVED or REJECTED. */
public record ReviewStatusUpdateRequest(@NotNull ReviewStatus status) {
}
