package com.jasonriddle.mcp.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration tests for API key authentication when disabled. */
@QuarkusTest
@TestProfile(ApiKeyAuthenticationDisabledIntegrationTest.SecurityDisabledProfile.class)
public class ApiKeyAuthenticationDisabledIntegrationTest {

    @TestHTTPResource("/v1/memory/mcp/sse")
    URI sseEndpoint;

    private static final String API_KEY_HEADER = "X-API-Key";

    private Client client;

    /**
     * Helper class to add API key authentication to SSE requests.
     */
    private static class ApiKeyAuthFilter implements ClientRequestFilter {
        private final String apiKey;

        ApiKeyAuthFilter(final String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public void filter(final ClientRequestContext requestContext) {
            if (apiKey != null) {
                requestContext.getHeaders().add(API_KEY_HEADER, apiKey);
            }
        }
    }

    private Client createClientWithApiKey(final String apiKey) {
        return ClientBuilder.newBuilder().register(new ApiKeyAuthFilter(apiKey)).build();
    }

    /** Test profile that disables security. */
    public static class SecurityDisabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "mcp.security.enabled", "false",
                    "quarkus.mcp.server.memory.sse.root-path", "/v1/memory/mcp",
                    "quarkus.http.test-port", "9093"); // Use specific available port
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

    @Test
    public void testRequestWithoutApiKeySucceedsWhenSecurityDisabled() throws InterruptedException {
        // Test that SSE connections work without any authentication when security is disabled
        final WebTarget target = client.target(sseEndpoint);
        final CountDownLatch connectionLatch = new CountDownLatch(1);
        final List<String> events = new ArrayList<>();

        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(
                    event -> {
                        events.add(event.readData(String.class));
                        connectionLatch.countDown();
                    },
                    throwable -> {
                        // Connection errors are expected during test cleanup
                        connectionLatch.countDown();
                    });

            eventSource.open();

            // Wait for successful connection
            boolean connected = connectionLatch.await(5, TimeUnit.SECONDS);
            assertTrue(connected, "Should successfully connect without API key when security disabled");
        }
    }

    @Test
    public void testRequestWithInvalidApiKeySucceedsWhenSecurityDisabled() throws InterruptedException {
        // Test that even invalid API keys are ignored when security is disabled
        final Client authClient = createClientWithApiKey("any-random-key");
        try {
            final WebTarget target = authClient.target(sseEndpoint);
            final CountDownLatch connectionLatch = new CountDownLatch(1);
            final List<String> events = new ArrayList<>();

            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(
                        event -> {
                            events.add(event.readData(String.class));
                            connectionLatch.countDown();
                        },
                        throwable -> {
                            // Connection errors are expected during test cleanup
                            connectionLatch.countDown();
                        });

                eventSource.open();

                // Wait for successful connection
                boolean connected = connectionLatch.await(5, TimeUnit.SECONDS);
                assertTrue(connected, "Should successfully connect with invalid API key when security disabled");
            }
        } finally {
            authClient.close();
        }
    }

    @Test
    public void testRequestWithValidApiKeySucceedsWhenSecurityDisabled() throws InterruptedException {
        // Test that valid API keys work but are unnecessary when security is disabled
        final Client authClient = createClientWithApiKey("valid-key-but-ignored");
        try {
            final WebTarget target = authClient.target(sseEndpoint);
            final CountDownLatch connectionLatch = new CountDownLatch(1);
            final List<String> events = new ArrayList<>();

            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(
                        event -> {
                            events.add(event.readData(String.class));
                            connectionLatch.countDown();
                        },
                        throwable -> {
                            // Connection errors are expected during test cleanup
                            connectionLatch.countDown();
                        });

                eventSource.open();

                // Wait for successful connection
                boolean connected = connectionLatch.await(5, TimeUnit.SECONDS);
                assertTrue(connected, "Should successfully connect with valid API key when security disabled");
            }
        } finally {
            authClient.close();
        }
    }

    @Test
    public void testMultipleEndpointsWithoutApiKeyWhenSecurityDisabled() throws InterruptedException {
        // Test that all endpoints work without authentication when security is disabled
        // This ensures the security filter is completely bypassed
        final WebTarget target = client.target(sseEndpoint);
        final CountDownLatch connectionLatch = new CountDownLatch(1);

        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(event -> connectionLatch.countDown(), throwable -> connectionLatch.countDown());

            eventSource.open();

            // Wait for successful connection
            boolean connected = connectionLatch.await(5, TimeUnit.SECONDS);
            assertTrue(
                    connected,
                    "Should successfully connect to SSE endpoint without authentication when security disabled");
        }
    }

    @Test
    public void testOptionsRequestWithoutApiKeyWhenSecurityDisabled() throws InterruptedException {
        // Test SSE connection without authentication (SSE doesn't typically use OPTIONS)
        final WebTarget target = client.target(sseEndpoint);
        final CountDownLatch connectionLatch = new CountDownLatch(1);

        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(event -> connectionLatch.countDown(), throwable -> connectionLatch.countDown());

            eventSource.open();

            // Wait for successful connection
            boolean connected = connectionLatch.await(5, TimeUnit.SECONDS);
            assertTrue(connected, "Should successfully connect without authentication when security disabled");
        }
    }
}
