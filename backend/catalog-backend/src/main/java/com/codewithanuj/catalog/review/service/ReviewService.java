package com.codewithanuj.catalog.review.service;

import com.codewithanuj.catalog.product.repository.ProductRepository;
import com.codewithanuj.catalog.review.dto.RatingSummary;
import com.codewithanuj.catalog.review.dto.ReviewCreateRequest;
import com.codewithanuj.catalog.review.dto.ReviewResponseDto;
import com.codewithanuj.catalog.review.model.Review;
import com.codewithanuj.catalog.review.model.ReviewStatus;
import com.codewithanuj.catalog.review.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    /** Public submission. Validates the product exists; stores the review as PENDING. */
    @Transactional
    public ReviewResponseDto submitReview(String productNumber, ReviewCreateRequest request) {
        if (!productRepository.existsById(productNumber)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + productNumber);
        }
        Review review = new Review(productNumber, request.rating(), request.comment().trim());
        return toDto(reviewRepository.save(review));
    }

    /** Storefront: approved reviews for a product, newest first. */
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getApprovedReviews(String productNumber, Pageable pageable) {
        return reviewRepository
                .findByProductNumberAndStatusOrderByCreatedAtDesc(productNumber, ReviewStatus.APPROVED, pageable)
                .map(this::toDto);
    }

    /** Admin: reviews in a given moderation state (e.g. PENDING queue), newest first. */
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviewsByStatus(ReviewStatus status, Pageable pageable) {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::toDto);
    }

    /** Admin: approve or reject a review. */
    @Transactional
    public ReviewResponseDto updateStatus(UUID id, ReviewStatus status) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Review not found: " + id));
        review.setStatus(status);
        return toDto(reviewRepository.save(review));
    }

    /** Admin: permanently remove a review. */
    @Transactional
    public void deleteReview(UUID id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found: " + id);
        }
        reviewRepository.deleteById(id);
    }

    /** Approved-only average + count for a product. */
    @Transactional(readOnly = true)
    public RatingSummary getRatingSummary(String productNumber) {
        ReviewRepository.RatingAggregate agg =
                reviewRepository.aggregateRating(productNumber, ReviewStatus.APPROVED);
        return new RatingSummary(agg.getAverage(), agg.getCount());
    }

    /**
     * Approved-only rating summaries for several products in one query. Products
     * without approved reviews are omitted from the map (the storefront treats a
     * missing entry as "no rating yet").
     */
    @Transactional(readOnly = true)
    public Map<String, RatingSummary> getRatingSummaries(Collection<String> productNumbers) {
        Map<String, RatingSummary> summaries = new LinkedHashMap<>();
        if (productNumbers == null || productNumbers.isEmpty()) {
            return summaries;
        }
        for (ReviewRepository.ProductRatingAggregate agg :
                reviewRepository.aggregateRatings(ReviewStatus.APPROVED, productNumbers)) {
            summaries.put(agg.getProductNumber(), new RatingSummary(agg.getAverage(), agg.getCount()));
        }
        return summaries;
    }

    /** Count of reviews awaiting moderation (for the admin badge). */
    @Transactional(readOnly = true)
    public long countPending() {
        return reviewRepository.countByStatus(ReviewStatus.PENDING);
    }

    private ReviewResponseDto toDto(Review review) {
        return new ReviewResponseDto(
                review.getId(),
                review.getProductNumber(),
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt()
        );
    }
}
