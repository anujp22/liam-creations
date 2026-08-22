package com.codewithanuj.catalog.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces the CSRF token to be materialised so {@code CookieCsrfTokenRepository} actually
 * writes the {@code XSRF-TOKEN} cookie.
 *
 * <p>Spring Security defers loading the token: {@code CsrfFilter} puts a lazy
 * {@code Supplier<CsrfToken>} on the request and the repository only saves the cookie
 * when something calls {@code getToken()}. Nothing in this app renders a form, so
 * without this filter the cookie is never written and the admin SPA has no token to
 * echo back — every mutating request would be rejected with 403.
 *
 * <p>It is registered after the authentication point but <em>before</em> the
 * authorization filter, on purpose: the login page's first call is
 * {@code GET /api/admin/me}, which answers 401 for a logged-out owner. That response
 * still has to carry the cookie, otherwise the subsequent login POST has no token.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only the admin routes check the token, so only they need the cookie handed
        // out. Without this every storefront response carried a Set-Cookie, which is
        // noise on public traffic and the kind of header that stops a CDN caching a
        // response it otherwise could.
        return !request.getRequestURI().startsWith("/api/admin/");
    }
}
