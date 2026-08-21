package com.codewithanuj.catalog.shared.exception;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Guards the {@code DispatcherType.ERROR} allowance in {@code SecurityConfig}.
 *
 * <p>Spring registers the security filter chain for the ERROR dispatch as well as
 * REQUEST. {@code anyRequest().denyAll()} therefore also rejected the internal
 * re-dispatch to {@code /error}, and the security layer overwrote whatever status the
 * application had already decided. Reproduced against a running stack: a bad enum value
 * in an admin request was resolved as 400 in the access log and arrived at the client as
 * a <strong>403 with an empty body</strong>. A wrong HTTP method did the same, and a
 * missing upload came back 401. All three gave the caller nothing to work with.
 *
 * <p>The filter chain is the thing under test here, so filters stay enabled — note that
 * {@link ValidationExceptionHandlerTest} runs with {@code addFilters = false}, which is
 * precisely why the original suite never saw any of this.
 */
// src/test/resources/application.properties replaces the main file rather than adding
// to it, so the CORS origins SecurityConfig needs are supplied here. Every other test
// is a slice that never builds the real filter chain — which is how the bug above
// survived a green suite.
@SpringBootTest(properties = {
        "cors.allowed-origin.dev=http://localhost:5173",
        "cors.allowed-origin.prod=http://localhost:5173",
        "app.public-base-url=http://localhost:5173",
        "app.storage=local",
})
@AutoConfigureMockMvc
class ErrorDispatchSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theErrorDispatchIsNotRejectedByTheSecurityChain() throws Exception {
        int status = mockMvc.perform(get("/error").with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    return request;
                }))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("the internal /error dispatch must not be turned into 401/403 — that "
                        + "overwrites the real status on its way to the client")
                .isNotIn(401, 403);
    }

    @Test
    void aNormalRequestToErrorIsStillDenied() throws Exception {
        // The allowance is scoped to the dispatcher type, not the path. A client cannot
        // ask for an ERROR dispatch, so /error must stay closed on a plain request.
        int status = mockMvc.perform(get("/error"))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("/error must not become publicly reachable")
                .isIn(401, 403);
    }

    @Test
    void adminRoutesAreStillProtected() throws Exception {
        // The regression that would matter most: permitting the error dispatch must not
        // have opened anything real.
        mockMvc.perform(get("/api/admin/me"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
