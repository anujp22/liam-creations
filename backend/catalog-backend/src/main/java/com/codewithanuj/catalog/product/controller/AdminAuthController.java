package com.codewithanuj.catalog.product.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * The admin session lifecycle: log in, check who you are, log out (A12, Option 3).
 *
 * <p>Login exchanges the password for a server-side session referenced by an
 * {@code HttpOnly} cookie, so the password crosses the wire exactly once and never
 * exists anywhere JavaScript can read it. See {@code SecurityConfig} for the cookie and
 * CSRF settings, and {@code docs/ADMIN_AUTH_OPTIONS.md} for why this shape was chosen.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;

    /**
     * Persisting the context is explicit in Spring Security 6 — authenticating alone
     * does not store anything, so without this call the login would succeed and the
     * very next request would be a 401.
     */
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /** Kept in step with {@code server.servlet.session.cookie.*} so logout clears the right cookie. */
    @Value("${server.servlet.session.cookie.name}")
    private String sessionCookieName;

    @Value("${app.session.cookie-secure:true}")
    private boolean cookieSecure;

    public AdminAuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest body,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(body.username(), body.password()));
        } catch (AuthenticationException ex) {
            // One message for both a wrong username and a wrong password: saying which
            // was wrong tells an attacker whether the username is worth guessing at.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }

        // Session fixation: discard whatever session id the caller arrived holding, so
        // an id planted before login can never become an authenticated one.
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        request.getSession(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return Map.of("username", authentication.getName());
    }

    /**
     * Real, server-side logout: the session is destroyed, so the cookie still sitting in
     * the browser (or copied out of it) refers to nothing. This is the thing HTTP Basic
     * could not do — there, "log out" only cleared browser storage.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        // Invalidating the session is what revokes access; expiring the cookie is
        // housekeeping, so the browser stops presenting an id that refers to nothing.
        // Attributes must match the original or the browser treats it as a different
        // cookie and keeps the old one.
        ResponseCookie cleared = ResponseCookie.from(sessionCookieName, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
    }

    /** Who the current session belongs to; 401 from the filter chain when there is none. */
    @GetMapping("/me")
    public Map<String, String> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Map.of("username", username);
    }
}
