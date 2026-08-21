package com.codewithanuj.catalog.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, ex) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\":401,\"message\":\"Unauthorized\"}");
        };

        // Skip Basic Auth processing entirely for public routes so that stale browser-cached
        // credentials can never block them. Admin routes still go through the filter normally.
        BasicAuthenticationFilter basicFilter = new BasicAuthenticationFilter(authManager, entryPoint) {
            @Override
            protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request) {
                String path = request.getRequestURI();
                boolean isGet = "GET".equals(request.getMethod());
                boolean isReviewSubmit = "POST".equals(request.getMethod())
                        && path.startsWith("/api/products/") && path.endsWith("/reviews");
                boolean isOrderSubmit = "POST".equals(request.getMethod()) && path.equals("/api/orders");
                return (isGet && path.startsWith("/api/products"))
                        || (isGet && path.startsWith("/api/reviews"))
                        || (isGet && path.startsWith("/uploads/"))
                        || (isGet && path.equals("/sitemap.xml"))
                        || isReviewSubmit
                        || isOrderSubmit
                        || path.equals("/actuator/health");
            }
        };

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll()
                )
                .addFilterAt(basicFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(new ApiRateLimitFilter(trustedProxyCount), BasicAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(corsOriginDev, corsOriginProd));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
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
