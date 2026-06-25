package com.codewithanuj.catalog.review.repository;

import com.codewithanuj.catalog.review.model.Review;
import com.codewithanuj.catalog.review.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Storefront: approved reviews for a product, newest first.
    Page<Review> findByProductNumberAndStatusOrderByCreatedAtDesc(
            String productNumber, ReviewStatus status, Pageable pageable);

    // Admin moderation queue (typically PENDING), newest first.
    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    long countByStatus(ReviewStatus status);

    // Batch aggregate for a set of products (approved only). Products with no
    // approved reviews are simply absent from the result.
    @Query("SELECT r.productNumber AS productNumber, AVG(r.rating) AS average, COUNT(r) AS count " +
           "FROM Review r WHERE r.status = :status AND r.productNumber IN :productNumbers " +
           "GROUP BY r.productNumber")
    List<ProductRatingAggregate> aggregateRatings(@Param("status") ReviewStatus status,
                                                  @Param("productNumbers") Collection<String> productNumbers);

    /** Projection for {@link #aggregateRatings}. */
    interface ProductRatingAggregate {
        String getProductNumber();
        Double getAverage();
        long getCount();
    }
}
