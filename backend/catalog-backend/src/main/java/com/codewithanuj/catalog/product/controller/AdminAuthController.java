package com.codewithanuj.catalog.product.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight endpoint the admin frontend calls to verify Basic Auth credentials.
 * Returns 200 with the username when authenticated as ADMIN; the security filter
 * chain returns 401 otherwise. Has no side effects.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    @GetMapping("/me")
    public Map<String, String> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Map.of("username", username);
    }
}
