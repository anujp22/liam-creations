package com.codewithanuj.catalog.review.controller;

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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    private ReviewResponseDto review(ReviewStatus status) {
        return new ReviewResponseDto(UUID.randomUUID(), "PRD-001", 5, "Great", status, Instant.now());
    }

    @Test
    void getReviewsDefaultsToPendingQueue() throws Exception {
        when(reviewService.getReviewsByStatus(eq(ReviewStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review(ReviewStatus.PENDING))));

        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(reviewService).getReviewsByStatus(eq(ReviewStatus.PENDING), any(Pageable.class));
    }

    @Test
    void getReviewsHonoursStatusParam() throws Exception {
        when(reviewService.getReviewsByStatus(eq(ReviewStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review(ReviewStatus.APPROVED))));

        mockMvc.perform(get("/api/admin/reviews").param("status", "APPROVED"))
                .andExpect(status().isOk());

        verify(reviewService).getReviewsByStatus(eq(ReviewStatus.APPROVED), any(Pageable.class));
    }

    @Test
    void pendingCountReturnsCount() throws Exception {
        when(reviewService.countPending()).thenReturn(4L);

        mockMvc.perform(get("/api/admin/reviews/pending-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void updateStatusApprovesReview() throws Exception {
        UUID id = UUID.randomUUID();
        when(reviewService.updateStatus(eq(id), eq(ReviewStatus.APPROVED)))
                .thenReturn(review(ReviewStatus.APPROVED));

        mockMvc.perform(patch("/api/admin/reviews/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void updateStatusReturns400WhenStatusMissing() throws Exception {
        mockMvc.perform(patch("/api/admin/reviews/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReviewReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/reviews/" + id))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(id);
    }
}
