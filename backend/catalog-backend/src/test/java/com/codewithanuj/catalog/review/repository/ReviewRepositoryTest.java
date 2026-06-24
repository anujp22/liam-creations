package com.codewithanuj.catalog.review.repository;

import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductCategory;
import com.codewithanuj.catalog.product.model.ProductStatus;
import com.codewithanuj.catalog.product.repository.ProductRepository;
import com.codewithanuj.catalog.review.model.Review;
import com.codewithanuj.catalog.review.model.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void seedProducts() {
        reviewRepository.deleteAll();
        productRepository.deleteAll();
        saveProduct("PRD-001");
        saveProduct("PRD-002");
        saveProduct("PRD-003");
    }

    private void saveProduct(String number) {
        productRepository.save(new Product(
                number, "Saree " + number, "Desc",
                new BigDecimal("5000.00"), "INR", ProductStatus.IN_STOCK, false, null, ProductCategory.BRIDAL_SAREES));
    }

    private void saveReview(String productNumber, int rating, ReviewStatus status) {
        Review review = new Review(productNumber, rating, "comment");
        review.setStatus(status);
        reviewRepository.save(review);
    }

    @Test
    void findByProductAndStatusReturnsOnlyApprovedForThatProduct() {
        saveReview("PRD-001", 5, ReviewStatus.APPROVED);
        saveReview("PRD-001", 4, ReviewStatus.APPROVED);
        saveReview("PRD-001", 1, ReviewStatus.PENDING);
        saveReview("PRD-002", 5, ReviewStatus.APPROVED);

        var page = reviewRepository.findByProductNumberAndStatusOrderByCreatedAtDesc(
                "PRD-001", ReviewStatus.APPROVED, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(r -> r.getStatus() == ReviewStatus.APPROVED);
        assertThat(page.getContent()).allMatch(r -> r.getProductNumber().equals("PRD-001"));
    }

    @Test
    void countByStatusCountsAcrossProducts() {
        saveReview("PRD-001", 5, ReviewStatus.APPROVED);
        saveReview("PRD-001", 1, ReviewStatus.PENDING);
        saveReview("PRD-002", 2, ReviewStatus.PENDING);

        assertThat(reviewRepository.countByStatus(ReviewStatus.PENDING)).isEqualTo(2);
        assertThat(reviewRepository.countByStatus(ReviewStatus.APPROVED)).isEqualTo(1);
    }

    @Test
    void aggregateRatingAveragesApprovedOnly() {
        saveReview("PRD-001", 4, ReviewStatus.APPROVED);
        saveReview("PRD-001", 5, ReviewStatus.APPROVED);
        saveReview("PRD-001", 1, ReviewStatus.PENDING); // excluded

        var agg = reviewRepository.aggregateRating("PRD-001", ReviewStatus.APPROVED);

        assertThat(agg.getAverage()).isEqualTo(4.5);
        assertThat(agg.getCount()).isEqualTo(2);
    }

    @Test
    void aggregateRatingsBatchesApprovedAndOmitsUnratedProducts() {
        saveReview("PRD-001", 4, ReviewStatus.APPROVED);
        saveReview("PRD-001", 5, ReviewStatus.APPROVED);
        saveReview("PRD-002", 3, ReviewStatus.APPROVED);
        saveReview("PRD-003", 5, ReviewStatus.PENDING); // no approved -> omitted

        List<ReviewRepository.ProductRatingAggregate> rows = reviewRepository.aggregateRatings(
                ReviewStatus.APPROVED, List.of("PRD-001", "PRD-002", "PRD-003"));

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(ReviewRepository.ProductRatingAggregate::getProductNumber)
                .containsExactlyInAnyOrder("PRD-001", "PRD-002");
    }
}
