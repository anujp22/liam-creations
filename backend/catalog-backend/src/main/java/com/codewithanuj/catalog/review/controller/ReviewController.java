package com.codewithanuj.catalog.review.controller;

import com.codewithanuj.catalog.review.dto.RatingSummary;
import com.codewithanuj.catalog.review.dto.ReviewCreateRequest;
import com.codewithanuj.catalog.review.dto.ReviewResponseDto;
import com.codewithanuj.catalog.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Public review endpoints: submit, list approved, and batch rating summaries. */
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/products/{productNumber}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponseDto submitReview(
            @PathVariable String productNumber,
            @Valid @RequestBody ReviewCreateRequest request) {
        return reviewService.submitReview(productNumber, request);
    }

    @GetMapping("/api/products/{productNumber}/reviews")
    public Page<ReviewResponseDto> getReviews(
            @PathVariable String productNumber,
            @PageableDefault(size = 10) Pageable pageable) {
        return reviewService.getApprovedReviews(productNumber, pageable);
    }

    // A grid page asks for at most ~20 products; cap well above that so an
    // unauthenticated caller can't force a huge IN (...) query.
    private static final int MAX_SUMMARY_PRODUCTS = 100;

    /** Approved-only rating summaries for the given products (e.g. a grid page). */
    @GetMapping("/api/reviews/summary")
    public Map<String, RatingSummary> getRatingSummaries(@RequestParam List<String> productNumbers) {
        List<String> capped = productNumbers.size() > MAX_SUMMARY_PRODUCTS
                ? productNumbers.subList(0, MAX_SUMMARY_PRODUCTS)
                : productNumbers;
        return reviewService.getRatingSummaries(capped);
    }
}
