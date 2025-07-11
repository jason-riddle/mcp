package com.jasonriddle.mcp.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.oidc.client.OidcTestClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration tests for SSE with OIDC authentication enabled. */
@QuarkusTest
@TestProfile(McpServerSseWithAuthIntegrationTest.AuthProfile.class)
public class McpServerSseWithAuthIntegrationTest {

    @TestHTTPResource("/v1/mcp/sse")
    URI sseEndpoint;

    private static final OidcTestClient OIDC_TEST_CLIENT = new OidcTestClient();
    private Client client;

    private static class BearerAuthFilter implements ClientRequestFilter {
        private final String token;

        BearerAuthFilter(final String token) {
            this.token = token;
        }

        @Override
        public void filter(final ClientRequestContext requestContext) {
            if (token != null) {
                requestContext.getHeaders().add("Authorization", "Bearer " + token);
            }
        }
    }

    private Client createAuthenticatedClient(final String token) {
        BearerAuthFilter authFilter = new BearerAuthFilter(token);
        return ClientBuilder.newBuilder().register(authFilter).build();
    }

    /** Test profile with SSE and OIDC enabled. */
    public static class AuthProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "memory.file.path", "memory-sse-auth-test.jsonl",
                    "weather.api.key", "test-api-key",
                    "quarkus.mcp.server.sse.root-path", "/v1/memory/mcp",
                    "quarkus.http.auth.permission.authenticated.paths", "/v1/mcp/sse",
                    "quarkus.http.auth.permission.authenticated.policy", "authenticated",
                    "quarkus.mcp.server.stdio.enabled", "false",
                    "quarkus.mcp.server.stdio.initialization-enabled", "false",
                    "quarkus.mcp.server.traffic-logging.enabled", "true");
        }

        @Override
        public String getConfigProfile() {
            return "sse-auth-test";
        }
    }

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @AfterAll
    static void closeClient() {
        OIDC_TEST_CLIENT.close();
    }

    @Test
    void shouldRejectWithoutToken() throws InterruptedException {
        WebTarget target = client.target(sseEndpoint);
        CountDownLatch latch = new CountDownLatch(1);
        try (SseEventSource es = SseEventSource.target(target).build()) {
            es.register(e -> latch.countDown(), t -> latch.countDown());
            es.open();
            latch.await(5, TimeUnit.SECONDS);
            assertFalse(es.isOpen(), "Connection should be rejected without token");
        }
    }

    @Test
    void shouldConnectWithToken() throws InterruptedException {
        String token = OIDC_TEST_CLIENT.getAccessToken("alice", "alice");
        Client authClient = createAuthenticatedClient(token);
        try {
            WebTarget target = authClient.target(sseEndpoint);
            CountDownLatch latch = new CountDownLatch(1);
            try (SseEventSource es = SseEventSource.target(target).build()) {
                es.register(event -> latch.countDown(), t -> latch.countDown());
                es.open();
                boolean connected = latch.await(5, TimeUnit.SECONDS);
                assertTrue(connected, "Should connect with valid token");
                assertTrue(es.isOpen(), "SSE connection should remain open with token");
            }
        } finally {
            authClient.close();
        }
    }
}
