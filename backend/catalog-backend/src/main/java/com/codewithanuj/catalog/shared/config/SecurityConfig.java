package com.codewithanuj.catalog.shared.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Admin authentication is a server-side session behind an {@code HttpOnly} cookie
 * (A12, Option 3 — see {@code docs/ADMIN_AUTH_OPTIONS.md}).
 *
 * <p>It replaced HTTP Basic, where the real password was base64-encoded into
 * {@code sessionStorage} and re-sent on every admin request. The cookie is
 * {@code HttpOnly}, so no JavaScript on the page can read it: an XSS hole in the admin
 * UI now steals a revocable session instead of the password itself. Logout is a real
 * server-side invalidation rather than clearing browser storage.
 *
 * <p>The cost is CSRF. Cookies are attached by the browser automatically, so a
 * cross-site form post would otherwise be authenticated; CSRF protection is therefore
 * <em>on</em> for admin routes, using the double-submit cookie the SPA echoes back as
 * {@code X-XSRF-TOKEN}. The public POSTs are exempt — they carry no credential of any
 * kind, so there is no authority for a forged request to borrow.
 */
@Configuration
@EnableWebSecurity
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class SecurityConfig {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${cors.allowed-origin.dev}")
    private String corsOriginDev;

    @Value("${cors.allowed-origin.prod}")
    private String corsOriginProd;

    /** Reverse proxies in front of the app; drives client-IP resolution for rate limiting. */
    @Value("${app.trusted-proxy-count:0}")
    private int trustedProxyCount;

    /**
     * Whether the session and CSRF cookies are marked {@code Secure}. True everywhere by
     * default, including local development: browsers treat {@code localhost} as a secure
     * context and accept {@code Secure} cookies over plain HTTP there. The escape hatch
     * exists only for a non-localhost HTTP environment, which production must never be.
     */
    @Value("${app.session.cookie-secure:true}")
    private boolean cookieSecure;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, ex) ->
                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");

        // Without this, a rejected CSRF token surfaces as Spring's own HTML 403 page.
        // The SPA parses `message` out of every failure, so it has to be JSON.
        AccessDeniedHandler accessDeniedHandler = (request, response, ex) ->
                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        // Opts out of the BREACH-mitigating XOR handler. That handler
                        // requires the client to send back the *masked* per-response
                        // token, which a JavaScript client reading a cookie cannot do.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Public writes: no cookie, no session, nothing to forge with.
                        // Leaving them protected would break the storefront for any
                        // visitor arriving without having first been issued a token.
                        .ignoringRequestMatchers("/api/orders", "/api/products/*/reviews")
                )
                // IF_REQUIRED, not ALWAYS: only AdminAuthController#login creates a
                // session, so storefront traffic stays session-free and the app keeps
                // scaling horizontally for everything except the single admin.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // Off, and it matters: the request cache saves the current request into a
                // new session on every 401 so it can be replayed after a form login. This
                // is a JSON API with no such redirect, so all it bought was a session per
                // anonymous request — anyone could have hung sessions on the server by
                // hitting /api/admin/me in a loop. Observed live before it was disabled:
                // a logged-out GET /api/admin/me came back 401 *and* Set-Cookie.
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        // Spring registers this filter chain for the ERROR dispatch as well as
                        // REQUEST. Without this line the internal re-dispatch to /error falls
                        // through to denyAll() below and the security layer overwrites whatever
                        // the app decided: a 400 from a bad request body reached the client as a
                        // 403 with an empty body, a 405 as a 403, and a missing upload as a 401.
                        // Verified against a running stack — the access log showed the true 400
                        // while curl received 403. ERROR is an internal dispatch type that no
                        // client can ask for, so permitting it grants no outside access; the
                        // response body is still whatever the handler chose to render.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/sitemap.xml").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/reviews").permitAll()
                        // Placing an order is public: there are no customer accounts, so
                        // requiring a login here would mean requiring one to buy anything.
                        // Rate-limited in ApiRateLimitFilter, which runs before this.
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // The one admin route reachable logged-out — it is how you log in.
                        // Still CSRF-protected, and rate-limited harder than the rest.
                        .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll()
                )
                .addFilterBefore(new ApiRateLimitFilter(trustedProxyCount), SecurityContextHolderFilter.class)
                .addFilterBefore(new CsrfCookieFilter(), AuthorizationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    /**
     * {@code withHttpOnlyFalse} is deliberate and is not a weakening of the session
     * cookie: the CSRF token is not a credential. It only proves the request came from
     * a page that could read same-origin state, which is exactly what the SPA must do
     * to attach the {@code X-XSRF-TOKEN} header.
     */
    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite("Strict"));
        return repository;
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setContentType("application/json");
        response.setStatus(status);
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(corsOriginDev, corsOriginProd));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
