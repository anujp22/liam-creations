package com.codewithanuj.catalog.review.controller;

import com.codewithanuj.catalog.review.dto.RatingSummary;
import com.codewithanuj.catalog.review.dto.ReviewResponseDto;
import com.codewithanuj.catalog.review.model.ReviewStatus;
import com.codewithanuj.catalog.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    private ReviewResponseDto approved(String productNumber, int rating, String comment) {
        return new ReviewResponseDto(UUID.randomUUID(), productNumber, rating, comment,
                ReviewStatus.APPROVED, Instant.now());
    }

    @Test
    void submitReviewReturns201() throws Exception {
        when(reviewService.submitReview(eq("PRD-001"), any()))
                .thenReturn(approved("PRD-001", 5, "Beautiful work"));

        mockMvc.perform(post("/api/products/PRD-001/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Beautiful work\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void submitReviewReturns400WhenRatingOutOfRange() throws Exception {
        mockMvc.perform(post("/api/products/PRD-001/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6,\"comment\":\"Nice\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("rating")));
    }

    @Test
    void submitReviewReturns400WhenCommentBlank() throws Exception {
        mockMvc.perform(post("/api/products/PRD-001/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("comment")));
    }

    @Test
    void getReviewsReturnsApprovedPage() throws Exception {
        when(reviewService.getApprovedReviews(eq("PRD-001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(approved("PRD-001", 5, "Great"))));

        mockMvc.perform(get("/api/products/PRD-001/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comment").value("Great"));
    }

    @Test
    void getRatingSummariesReturnsMap() throws Exception {
        when(reviewService.getRatingSummaries(anyCollection()))
                .thenReturn(Map.of("PRD-001", new RatingSummary(4.5, 2L)));

        mockMvc.perform(get("/api/reviews/summary").param("productNumbers", "PRD-001", "PRD-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PRD-001.average").value(4.5))
                .andExpect(jsonPath("$.PRD-001.count").value(2));
    }
}
