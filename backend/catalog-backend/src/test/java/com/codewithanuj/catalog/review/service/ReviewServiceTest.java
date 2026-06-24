package com.codewithanuj.catalog.review.service;

import com.codewithanuj.catalog.product.repository.ProductRepository;
import com.codewithanuj.catalog.review.dto.RatingSummary;
import com.codewithanuj.catalog.review.dto.ReviewCreateRequest;
import com.codewithanuj.catalog.review.dto.ReviewResponseDto;
import com.codewithanuj.catalog.review.model.Review;
import com.codewithanuj.catalog.review.model.ReviewStatus;
import com.codewithanuj.catalog.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void submitReviewStoresPendingTrimmedReviewWhenProductExists() {
        when(productRepository.existsById("PRD-001")).thenReturn(true);
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponseDto dto = reviewService.submitReview("PRD-001", new ReviewCreateRequest(5, "  Lovely!  "));

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(saved.getValue().getComment()).isEqualTo("Lovely!");
        assertThat(saved.getValue().getRating()).isEqualTo(5);
        assertThat(dto.rating()).isEqualTo(5);
    }

    @Test
    void submitReviewThrows404WhenProductMissing() {
        when(productRepository.existsById("PRD-999")).thenReturn(false);

        assertThatThrownBy(() -> reviewService.submitReview("PRD-999", new ReviewCreateRequest(4, "Nice")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found: PRD-999");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void getApprovedReviewsQueriesApprovedStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findByProductNumberAndStatusOrderByCreatedAtDesc("PRD-001", ReviewStatus.APPROVED, pageable))
                .thenReturn(new PageImpl<>(List.of(new Review("PRD-001", 5, "Great"))));

        Page<ReviewResponseDto> result = reviewService.getApprovedReviews("PRD-001", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void updateStatusApprovesAndSaves() {
        Review review = new Review("PRD-001", 4, "Good");
        UUID id = UUID.randomUUID();
        when(reviewRepository.findById(id)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponseDto dto = reviewService.updateStatus(id, ReviewStatus.APPROVED);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(dto.status()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    void updateStatusThrows404WhenMissing() {
        UUID id = UUID.randomUUID();
        when(reviewRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateStatus(id, ReviewStatus.APPROVED))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    void deleteReviewRemovesWhenExists() {
        UUID id = UUID.randomUUID();
        when(reviewRepository.existsById(id)).thenReturn(true);

        reviewService.deleteReview(id);

        verify(reviewRepository).deleteById(id);
    }

    @Test
    void deleteReviewThrows404WhenMissing() {
        UUID id = UUID.randomUUID();
        when(reviewRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.deleteReview(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Review not found");
        verify(reviewRepository, never()).deleteById(any());
    }

    @Test
    void getRatingSummaryReturnsAverageAndCount() {
        ReviewRepository.RatingAggregate agg = mock(ReviewRepository.RatingAggregate.class);
        when(agg.getAverage()).thenReturn(4.5);
        when(agg.getCount()).thenReturn(2L);
        when(reviewRepository.aggregateRating("PRD-001", ReviewStatus.APPROVED)).thenReturn(agg);

        RatingSummary summary = reviewService.getRatingSummary("PRD-001");

        assertThat(summary.average()).isEqualTo(4.5);
        assertThat(summary.count()).isEqualTo(2);
    }

    @Test
    void getRatingSummariesBuildsMapAndSkipsUnratedProducts() {
        ReviewRepository.ProductRatingAggregate agg = mock(ReviewRepository.ProductRatingAggregate.class);
        when(agg.getProductNumber()).thenReturn("PRD-001");
        when(agg.getAverage()).thenReturn(5.0);
        when(agg.getCount()).thenReturn(1L);
        when(reviewRepository.aggregateRatings(eq(ReviewStatus.APPROVED), anyCollection()))
                .thenReturn(List.of(agg));

        Map<String, RatingSummary> summaries = reviewService.getRatingSummaries(List.of("PRD-001", "PRD-002"));

        assertThat(summaries).containsOnlyKeys("PRD-001");
        assertThat(summaries.get("PRD-001").average()).isEqualTo(5.0);
    }

    @Test
    void getRatingSummariesReturnsEmptyForEmptyInput() {
        assertThat(reviewService.getRatingSummaries(List.of())).isEmpty();
        verify(reviewRepository, never()).aggregateRatings(any(), anyCollection());
    }

    @Test
    void countPendingDelegatesToRepository() {
        when(reviewRepository.countByStatus(ReviewStatus.PENDING)).thenReturn(3L);

        assertThat(reviewService.countPending()).isEqualTo(3);
    }
}
