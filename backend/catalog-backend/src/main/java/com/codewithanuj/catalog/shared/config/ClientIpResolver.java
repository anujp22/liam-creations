package com.codewithanuj.catalog.shared.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the originating client IP for rate limiting when the app sits behind
 * reverse proxies.
 *
 * <p>{@code X-Forwarded-For} is append-only and <em>partly attacker-controlled</em>:
 * a client can send any prefix it likes, and each proxy appends the address it saw.
 * For a chain of {@code client → CloudFront → ALB → app} the header arrives as:
 *
 * <pre>
 *   X-Forwarded-For: &lt;anything the client made up&gt;, &lt;real client IP&gt;, &lt;CloudFront edge IP&gt;
 *                     \___ untrusted, ignored ___/   \_ appended by CloudFront _/  \_ by ALB _/
 * </pre>
 *
 * <p>So the trustworthy entry is counted <strong>from the right</strong>, one position
 * per proxy we actually run behind — never the leftmost value, which is exactly the
 * part a client can forge. With two proxies the client IP is the second-from-last
 * entry, no matter how many junk entries were prepended.
 *
 * <p>{@code trustedProxyCount} must match the real deployment depth. Too low and we
 * key on a proxy address (everyone shares a bucket); too high and we key on a value
 * the client controls (the limit becomes trivially evadable). It defaults to 0 for
 * local development, where there is no proxy and the socket address is the truth.
 *
 * <p>Note: this deliberately reads the header itself rather than relying on
 * {@code server.forward-headers-strategy}. Spring's {@code ForwardedHeaderFilter}
 * derives {@code getRemoteAddr()} from the <em>leftmost</em> forwarded entry and then
 * strips the headers, which would leave this filter keying on a spoofable value. If a
 * forwarded-headers strategy is ever enabled, revisit this class first.
 */
final class ClientIpResolver {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final int trustedProxyCount;

    ClientIpResolver(int trustedProxyCount) {
        this.trustedProxyCount = Math.max(0, trustedProxyCount);
    }

    String resolve(HttpServletRequest request) {
        if (trustedProxyCount == 0) {
            return request.getRemoteAddr();
        }

        String header = request.getHeader(FORWARDED_FOR);
        if (header == null || header.isBlank()) {
            // No forwarded chain: the request did not arrive through the expected
            // proxies. The socket address is the only non-forgeable value we have.
            return request.getRemoteAddr();
        }

        String[] entries = header.split(",");
        int index = entries.length - trustedProxyCount;
        if (index < 0) {
            // Shorter chain than configured — a direct hit on the load balancer, or a
            // misconfigured proxy count. Falling back to the socket address keeps the
            // key honest rather than trusting a client-supplied entry.
            return request.getRemoteAddr();
        }

        String candidate = entries[index].trim();
        return candidate.isEmpty() ? request.getRemoteAddr() : candidate;
    }
}
