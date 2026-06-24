package com.codewithanuj.catalog.review.repository;

import com.codewithanuj.catalog.review.model.Review;
import com.codewithanuj.catalog.review.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Storefront: approved reviews for a product, newest first.
    Page<Review> findByProductNumberAndStatusOrderByCreatedAtDesc(
            String productNumber, ReviewStatus status, Pageable pageable);

    // Admin moderation queue (typically PENDING), newest first.
    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    long countByStatus(ReviewStatus status);

    // Aggregate rating for a single product (approved only). average is null when
    // the product has no approved reviews.
    @Query("SELECT AVG(r.rating) AS average, COUNT(r) AS count FROM Review r " +
           "WHERE r.productNumber = :productNumber AND r.status = :status")
    RatingAggregate aggregateRating(@Param("productNumber") String productNumber,
                                    @Param("status") ReviewStatus status);

    /** Projection for {@link #aggregateRating}. */
    interface RatingAggregate {
        Double getAverage();
        long getCount();
    }
}
