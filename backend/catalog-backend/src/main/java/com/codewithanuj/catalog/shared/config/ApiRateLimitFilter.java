package com.codewithanuj.catalog.shared.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Per-IP rate limiting for abuse-prone routes, registered inside the security
 * filter chain ahead of authentication so abusive callers are throttled before
 * any auth work happens:
 * <ul>
 *   <li>{@code /api/admin/**} — 60 requests/min (authenticated admin traffic).</li>
 *   <li>{@code POST /api/products/&#42;/reviews} — 5/min (public, unauthenticated
 *       review submissions; the strictest limit since there is no login).</li>
 * </ul>
 *
 * <p>Buckets live in a Caffeine cache that expires idle keys after 10 minutes
 * and caps total entries, so the map can't grow unbounded under many client IPs.
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final int ADMIN_PER_MINUTE = 60;
    private static final int REVIEW_PER_MINUTE = 5;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String ip = request.getRemoteAddr();

        Bucket bucket;
        if (uri.startsWith("/api/admin/")) {
            bucket = bucketFor("admin:" + ip, ADMIN_PER_MINUTE);
        } else if (isReviewSubmit(request, uri)) {
            bucket = bucketFor("review:" + ip, REVIEW_PER_MINUTE);
        } else {
            chain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Please try again shortly."}
                    """);
        }
    }

    private boolean isReviewSubmit(HttpServletRequest request, String uri) {
        return "POST".equals(request.getMethod())
                && uri.startsWith("/api/products/") && uri.endsWith("/reviews");
    }

    private Bucket bucketFor(String key, int perMinute) {
        return buckets.get(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                .build());
    }
}
