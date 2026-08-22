package com.codewithanuj.catalog.product.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The controller's own behaviour, with the filter chain switched off. The chain itself
 * — cookie attributes, CSRF, who may reach what — is covered by
 * {@code AdminSessionAuthTest}, which runs the real thing.
 */
@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    private static final String GOOD_LOGIN = "{\"username\":\"admin\",\"password\":\"right\"}";

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void meReturnsUsernameOfAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void loginStoresTheSecurityContextInTheSession() throws Exception {
        given(authenticationManager.authenticate(any())).willReturn(authenticated());

        MvcResult result = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GOOD_LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(SPRING_SECURITY_CONTEXT_KEY))
                .as("saving the context is explicit in Spring Security 6 — without it the "
                        + "login succeeds and the very next request is a 401")
                .isNotNull();
    }

    @Test
    void loginDiscardsAnyPreExistingSession() throws Exception {
        given(authenticationManager.authenticate(any())).willReturn(authenticated());
        MockHttpSession planted = new MockHttpSession();

        mockMvc.perform(post("/api/admin/login")
                        .session(planted)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GOOD_LOGIN))
                .andExpect(status().isOk());

        assertThat(planted.isInvalid())
                .as("session fixation: an id planted before login must not become the "
                        + "authenticated one")
                .isTrue();
    }

    @Test
    void badCredentialsAreUnauthorizedAndSayNothingUseful() throws Exception {
        willThrow(new BadCredentialsException("Bad credentials"))
                .given(authenticationManager).authenticate(any());

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                // One message for both halves: naming which one was wrong tells an
                // attacker whether the username is worth guessing at.
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void aBlankPasswordIsRejectedBeforeAuthenticationIsAttempted() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void logoutInvalidatesTheSessionAndClearsTheContext() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MvcResult result = mockMvc.perform(post("/api/admin/logout").session(session))
                .andExpect(status().isNoContent())
                .andReturn();

        // Housekeeping, not the revocation: the browser should stop presenting an id
        // that now refers to nothing.
        Cookie cleared = result.getResponse().getCookie("LC_SESSION");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void logoutIsHarmlessWhenThereIsNoSession() throws Exception {
        mockMvc.perform(post("/api/admin/logout")).andExpect(status().isNoContent());
    }

    private static UsernamePasswordAuthenticationToken authenticated() {
        return new UsernamePasswordAuthenticationToken(
                "admin", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
    }
}
