package com.codewithanuj.catalog.review.controller;

import com.codewithanuj.catalog.review.dto.ReviewResponseDto;
import com.codewithanuj.catalog.review.dto.ReviewStatusUpdateRequest;
import com.codewithanuj.catalog.review.model.ReviewStatus;
import com.codewithanuj.catalog.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** Admin moderation for reviews: queue, approve/reject, delete, pending count. */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Moderation queue. Defaults to PENDING. */
    @GetMapping
    public Page<ReviewResponseDto> getReviews(
            @RequestParam(defaultValue = "PENDING") ReviewStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.getReviewsByStatus(status, pageable);
    }

    @GetMapping("/pending-count")
    public Map<String, Long> pendingCount() {
        return Map.of("count", reviewService.countPending());
    }

    /** Approve or reject a review. */
    @PatchMapping("/{id}")
    public ReviewResponseDto updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewStatusUpdateRequest request) {
        return reviewService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable UUID id) {
        reviewService.deleteReview(id);
    }
}
