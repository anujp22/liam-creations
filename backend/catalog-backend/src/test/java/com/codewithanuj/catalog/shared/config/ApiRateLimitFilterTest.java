package com.codewithanuj.catalog.shared.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRateLimitFilterTest {

    private final ApiRateLimitFilter filter = new ApiRateLimitFilter(0);

    /** A filter configured as it runs in prod: behind CloudFront and an ALB. */
    private final ApiRateLimitFilter behindProxies = new ApiRateLimitFilter(2);

    private MockHttpServletResponse submitReview(String ip) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products/PRD-001/reviews");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    /**
     * Sends a review submit through the two-proxy filter. {@code remoteAddr} is the ALB
     * (as the app would see it) and {@code forwardedFor} is the header as it arrives.
     */
    private MockHttpServletResponse submitForwarded(String remoteAddr, String forwardedFor)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products/PRD-001/reviews");
        request.setRemoteAddr(remoteAddr);
        request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        behindProxies.doFilter(request, response, new MockFilterChain());
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

    // ── behind a proxy chain (client → CloudFront → ALB → app) ───────────────────

    @Test
    void behindProxiesTheLimitStillAppliesPerClientNotPerLoadBalancer() throws Exception {
        // Two visitors arriving through the same ALB and CloudFront edge.
        for (int i = 1; i <= 5; i++) {
            assertThat(submitForwarded("10.0.0.7", "203.0.113.9, 70.1.1.1").getStatus()).isNotEqualTo(429);
        }
        assertThat(submitForwarded("10.0.0.7", "203.0.113.9, 70.1.1.1").getStatus()).isEqualTo(429);

        // The second visitor shares the proxy chain but must keep a full allowance —
        // this is the bug that let one submitter lock out the entire internet.
        assertThat(submitForwarded("10.0.0.7", "198.51.100.4, 70.1.1.1").getStatus()).isNotEqualTo(429);
    }

    @Test
    void forgedLeadingForwardedForEntriesCannotEvadeTheLimit() throws Exception {
        // The client prepends junk to X-Forwarded-For, varying it every request. Only the
        // entries proxies append are trusted, so all six land in the same bucket.
        for (int i = 1; i <= 5; i++) {
            assertThat(submitForwarded("10.0.0.7", "9.9.9." + i + ", 203.0.113.9, 70.1.1.1").getStatus())
                    .isNotEqualTo(429);
        }
        assertThat(submitForwarded("10.0.0.7", "9.9.9.99, 203.0.113.9, 70.1.1.1").getStatus())
                .isEqualTo(429);
    }

    @Test
    void fallsBackToSocketAddressWhenTheForwardedChainIsShorterThanConfigured() throws Exception {
        // A direct hit on the load balancer: too few entries to locate a client entry, so
        // the non-forgeable socket address is used rather than a client-supplied value.
        for (int i = 1; i <= 5; i++) {
            assertThat(submitForwarded("10.0.0.7", "5.5.5.5").getStatus()).isNotEqualTo(429);
        }
        assertThat(submitForwarded("10.0.0.7", "6.6.6.6").getStatus()).isEqualTo(429);
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
