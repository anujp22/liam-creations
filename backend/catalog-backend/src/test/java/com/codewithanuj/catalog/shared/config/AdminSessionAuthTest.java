package com.codewithanuj.catalog.shared.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The A12 filter chain: session-cookie login, and the CSRF protection that has to come
 * with it. Filters stay enabled — every controller slice in this suite runs with
 * {@code addFilters = false}, so this is the only place any of it is actually exercised.
 *
 * <p>CSRF is the part the memo said to test hardest, and for good reason: cookies are
 * attached by the browser whether or not the request came from our own page, so if the
 * token check were mis-wired the admin API would be open to any site the owner visits
 * while logged in. The tokens below are obtained the way the browser does — read the
 * {@code XSRF-TOKEN} cookie, echo it in {@code X-XSRF-TOKEN} — rather than through
 * Spring's {@code csrf()} post-processor, which would bypass the wiring under test.
 */
@SpringBootTest(properties = {
        "cors.allowed-origin.dev=http://localhost:5173",
        "cors.allowed-origin.prod=http://localhost:5173",
        "app.public-base-url=http://localhost:5173",
        "app.storage=local",
})
@AutoConfigureMockMvc
class AdminSessionAuthTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    // ── the cookie itself ─────────────────────────────────────────────────────

    @Test
    void theSessionCookieIsHttpOnlySecureAndSameSiteStrict() throws Exception {
        // Read from the file rather than the environment on purpose. Tomcat writes this
        // cookie and MockMvc has no Tomcat, and src/test/resources/application.properties
        // replaces the main file rather than adding to it — so the running test context
        // cannot answer this question. The file is what production boots on, and HttpOnly
        // is the entire reason this option was chosen over a bearer token: one deleted
        // line and the session becomes readable by any script on the page.
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(properties)
                .as("HttpOnly — JavaScript must not be able to read the session")
                .contains("server.servlet.session.cookie.http-only=true")
                .as("Secure — the session must never travel in clear text")
                .contains("server.servlet.session.cookie.secure=")
                .as("SameSite=Strict — never attached to a cross-site request")
                .contains("server.servlet.session.cookie.same-site=strict");
        assertThat(properties)
                .as("the Secure default must be on; the env var is an escape hatch, not a switch")
                .contains("server.servlet.session.cookie.secure=${SESSION_COOKIE_SECURE:true}");
    }

    @Test
    void anUnauthenticatedRequestStillHandsOutACsrfToken() throws Exception {
        // The login page's first call is GET /api/admin/me, which answers 401. If that
        // response did not carry the token cookie, the login POST after it would have
        // nothing to send and the owner could never get in.
        MvcResult result = mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Cookie token = result.getResponse().getCookie(CSRF_COOKIE);
        assertThat(token).isNotNull();
        assertThat(token.getValue()).isNotBlank();
        assertThat(token.isHttpOnly()).as("the SPA has to read this one").isFalse();
        assertThat(token.getSecure()).isTrue();
        assertThat(token.getAttribute("SameSite")).isEqualTo("Strict");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void loginWithoutACsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithBadCredentialsIsUnauthorizedAndCreatesNoSession() throws Exception {
        Cookie token = freshCsrfToken();

        MvcResult result = mockMvc.perform(post("/api/admin/login")
                        .cookie(token)
                        .header(CSRF_HEADER, token.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getRequest().getSession(false))
                .as("a failed login must not leave an authenticated session behind")
                .isNull();
    }

    @Test
    void loginWithGoodCredentialsAuthenticatesLaterRequests() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/api/admin/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void loginReplacesAnyPreExistingSession() throws Exception {
        // Session fixation: an id planted in the browser before login must not be the
        // id that ends up authenticated.
        MockHttpSession planted = new MockHttpSession();
        Cookie token = freshCsrfToken();

        MvcResult result = mockMvc.perform(post("/api/admin/login")
                        .session(planted)
                        .cookie(token)
                        .header(CSRF_HEADER, token.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(planted.isInvalid()).isTrue();
        assertThat(result.getRequest().getSession(false)).isNotSameAs(planted);
    }

    // ── CSRF on the authenticated routes ──────────────────────────────────────

    @Test
    void anAdminWriteWithACookieButNoCsrfTokenIsRejected() throws Exception {
        // The whole reason CSRF had to come back. The browser attaches the session
        // cookie to a cross-site form post automatically; only the token the attacker
        // cannot read stops it.
        MockHttpSession session = login();

        mockMvc.perform(delete("/api/admin/products/LC-0001").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdminReadWithACookieAndNoCsrfTokenIsAllowed() throws Exception {
        // GET is CSRF-safe by definition; protecting it would break every screen.
        MockHttpSession session = login();

        mockMvc.perform(get("/api/admin/metrics").session(session))
                .andExpect(status().isOk());
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logoutInvalidatesTheSessionServerSide() throws Exception {
        MockHttpSession session = login();
        Cookie token = freshCsrfToken();

        mockMvc.perform(post("/api/admin/logout")
                        .session(session)
                        .cookie(token)
                        .header(CSRF_HEADER, token.getValue()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid())
                .as("this is what HTTP Basic could not do — the credential is revoked, "
                        + "not just forgotten by the browser")
                .isTrue();
    }

    // ── what must not have regressed ──────────────────────────────────────────

    @Test
    void basicAuthNoLongerGrantsAdminAccess() throws Exception {
        String basic = java.util.Base64.getEncoder().encodeToString("admin:test".getBytes());

        mockMvc.perform(get("/api/admin/me").header("Authorization", "Basic " + basic))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void thePublicOrderPostIsNotBlockedByCsrf() throws Exception {
        // Exempted on purpose: a shopper arrives with no session and no token, and
        // there is no authority for a forged request to borrow. A rejected body must
        // come back as a validation error, never as a CSRF 403.
        int status = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).as("the storefront checkout must still reach the controller").isNotEqualTo(403);
    }

    @Test
    void thePublicReviewPostIsNotBlockedByCsrf() throws Exception {
        int status = mockMvc.perform(post("/api/products/LC-0001/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotEqualTo(403);
    }

    @Test
    void anAnonymousRejectionCreatesNoSession() throws Exception {
        // Spring's request cache would otherwise open a session on every 401 just to
        // replay the request after a form login that does not exist here — which means
        // an unauthenticated caller could allocate server-side state at will.
        MvcResult result = mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void theStorefrontIsNotHandedACsrfCookie() throws Exception {
        // Public traffic never sends the token, so setting one on every product page
        // request would be a cookie nobody uses on a response a CDN might want to cache.
        MvcResult result = mockMvc.perform(get("/api/products")).andReturn();

        assertThat(result.getResponse().getCookie(CSRF_COOKIE)).isNull();
    }

    @Test
    void theStorefrontDoesNotGetASession() throws Exception {
        // IF_REQUIRED, not ALWAYS: only login creates a session, so public traffic costs
        // no server-side state and stays horizontally scalable.
        MvcResult result = mockMvc.perform(get("/api/products")).andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Reads the token cookie the way the browser does, from any response. */
    private Cookie freshCsrfToken() throws Exception {
        Cookie token = mockMvc.perform(get("/api/admin/me")).andReturn().getResponse().getCookie(CSRF_COOKIE);
        assertThat(token).isNotNull();
        return token;
    }

    /** Logs in as the test admin and returns the session the server created. */
    private MockHttpSession login() throws Exception {
        Cookie token = freshCsrfToken();

        MvcResult result = mockMvc.perform(post("/api/admin/login")
                        .cookie(token)
                        .header(CSRF_HEADER, token.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }
}
