package com.codewithanuj.catalog.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public review submission: a 1–5 rating and a comment. No name/PII collected. */
public record ReviewCreateRequest(
        @Min(value = 1, message = "rating must be between 1 and 5")
        @Max(value = 5, message = "rating must be between 1 and 5")
        int rating,

        @NotBlank
        @Size(max = 2000, message = "comment must be at most 2000 characters")
        String comment
) {
}
