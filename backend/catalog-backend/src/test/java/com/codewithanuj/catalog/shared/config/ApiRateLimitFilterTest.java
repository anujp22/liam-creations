package com.codewithanuj.catalog.shared.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRateLimitFilterTest {

    private final ApiRateLimitFilter filter = new ApiRateLimitFilter();

    private MockHttpServletResponse submitReview(String ip) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products/PRD-001/reviews");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void allowsUpToFiveReviewSubmitsThenReturns429() throws Exception {
        for (int i = 1; i <= 5; i++) {
            assertThat(submitReview("1.2.3.4").getStatus()).isNotEqualTo(429);
        }
        MockHttpServletResponse sixth = submitReview("1.2.3.4");
        assertThat(sixth.getStatus()).isEqualTo(429);
        assertThat(sixth.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void limitIsPerIp() throws Exception {
        for (int i = 1; i <= 5; i++) {
            submitReview("1.1.1.1");
        }
        // A different IP still has its full allowance.
        assertThat(submitReview("2.2.2.2").getStatus()).isNotEqualTo(429);
    }

    @Test
    void doesNotThrottleNonAbuseRoutes() throws Exception {
        for (int i = 1; i <= 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
            request.setRemoteAddr("9.9.9.9");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isNotEqualTo(429);
        }
    }
}
