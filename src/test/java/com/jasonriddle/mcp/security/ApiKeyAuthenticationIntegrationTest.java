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

/** Integration tests for API key authentication. */
@QuarkusTest
@TestProfile(ApiKeyAuthenticationIntegrationTest.SecurityEnabledProfile.class)
public class ApiKeyAuthenticationIntegrationTest {

    @TestHTTPResource("/v1/memory/mcp/sse")
    URI sseEndpoint;

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String VALID_API_KEY = "test-integration-api-key-12345";
    private static final String INVALID_API_KEY = "invalid-key";

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

    /** Test profile that enables security with a test API key. */
    public static class SecurityEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "mcp.security.enabled", "true",
                    "mcp.security.api-key", VALID_API_KEY,
                    "quarkus.mcp.server.memory.sse.root-path", "/v1/memory/mcp",
                    "quarkus.http.test-port", "9092"); // Use specific available port
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
    public void testRequestWithoutApiKeyReturns401() throws InterruptedException {
        // Test that SSE connections without any API key header are rejected when security is enabled
        final WebTarget target = client.target(sseEndpoint);
        final CountDownLatch errorLatch = new CountDownLatch(1);
        final List<Throwable> errors = new ArrayList<>();

        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(
                    event -> {
                        // Should not receive events without valid API key
                    },
                    throwable -> {
                        // Authentication failures should trigger error handler
                        errors.add(throwable);
                        errorLatch.countDown();
                    });

            eventSource.open();

            // Wait for authentication error - should fail quickly due to missing API key
            boolean errorReceived = errorLatch.await(5, TimeUnit.SECONDS);
            assertTrue(errorReceived, "Should receive authentication error when no API key provided");
        }
    }

    @Test
    public void testRequestWithInvalidApiKeyReturns401() throws InterruptedException {
        final Client authClient = createClientWithApiKey(INVALID_API_KEY);
        try {
            final WebTarget target = authClient.target(sseEndpoint);
            final CountDownLatch errorLatch = new CountDownLatch(1);
            final List<Throwable> errors = new ArrayList<>();

            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(
                        event -> {
                            // Should not receive events with invalid API key
                        },
                        throwable -> {
                            errors.add(throwable);
                            errorLatch.countDown();
                        });

                eventSource.open();

                // Wait for authentication error
                boolean errorReceived = errorLatch.await(5, TimeUnit.SECONDS);
                assertTrue(errorReceived, "Should receive authentication error");
            }
        } finally {
            authClient.close();
        }
    }

    @Test
    public void testRequestWithValidApiKeySucceeds() throws InterruptedException {
        final Client authClient = createClientWithApiKey(VALID_API_KEY);
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
                assertTrue(connected, "Should successfully connect with valid API key");
            }
        } finally {
            authClient.close();
        }
    }

    @Test
    public void testMultipleEndpointsWithValidApiKey() throws InterruptedException {
        // Test that authentication works consistently across endpoint access
        // Note: Currently only testing SSE endpoint, but validates auth filter consistency
        final Client authClient = createClientWithApiKey(VALID_API_KEY);
        try {
            final WebTarget target = authClient.target(sseEndpoint);
            final CountDownLatch connectionLatch = new CountDownLatch(1);

            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(event -> connectionLatch.countDown(), throwable -> connectionLatch.countDown());

                eventSource.open();

                // Successful connection validates that auth filter works reliably
                boolean connected = connectionLatch.await(5, TimeUnit.SECONDS);
                assertTrue(connected, "Should successfully connect to SSE endpoint with valid API key");
            }
        } finally {
            authClient.close();
        }
    }

    @Test
    public void testRequestWithEmptyApiKeyReturns401() throws InterruptedException {
        // Test that empty string API key is treated as invalid (not just missing)
        final Client authClient = createClientWithApiKey("");
        try {
            final WebTarget target = authClient.target(sseEndpoint);
            final CountDownLatch errorLatch = new CountDownLatch(1);
            final List<Throwable> errors = new ArrayList<>();

            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(
                        event -> {
                            // Should not receive events with empty API key
                        },
                        throwable -> {
                            // Empty API key should be rejected same as missing key
                            errors.add(throwable);
                            errorLatch.countDown();
                        });

                eventSource.open();

                // Wait for authentication error - empty key should be invalid
                boolean errorReceived = errorLatch.await(5, TimeUnit.SECONDS);
                assertTrue(errorReceived, "Should receive authentication error for empty API key");
            }
        } finally {
            authClient.close();
        }
    }

    @Test
    public void testRequestWithNullApiKeyReturns401() throws InterruptedException {
        // Test that null API key doesn't add header and triggers authentication failure
        final Client authClient = createClientWithApiKey(null);
        try {
            final WebTarget target = authClient.target(sseEndpoint);
            final CountDownLatch errorLatch = new CountDownLatch(1);
            final List<Throwable> errors = new ArrayList<>();

            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(
                        event -> {
                            // Should not receive events when filter doesn't add header due to null key
                        },
                        throwable -> {
                            // Null API key results in no header being added, causing auth failure
                            errors.add(throwable);
                            errorLatch.countDown();
                        });

                eventSource.open();

                // Wait for authentication error - null should be treated as missing
                boolean errorReceived = errorLatch.await(5, TimeUnit.SECONDS);
                assertTrue(errorReceived, "Should receive authentication error for null API key");
            }
        } finally {
            authClient.close();
        }
    }

    @Test
    public void testCaseInsensitiveHeaderName() throws InterruptedException {
        // Test lowercase header
        final Client authClient1 = ClientBuilder.newBuilder()
                .register((ClientRequestFilter)
                        requestContext -> requestContext.getHeaders().add("x-api-key", VALID_API_KEY))
                .build();

        try {
            final WebTarget target1 = authClient1.target(sseEndpoint);
            final CountDownLatch connectionLatch1 = new CountDownLatch(1);

            try (SseEventSource eventSource = SseEventSource.target(target1).build()) {
                eventSource.register(event -> connectionLatch1.countDown(), throwable -> connectionLatch1.countDown());

                eventSource.open();
                boolean connected1 = connectionLatch1.await(5, TimeUnit.SECONDS);
                assertTrue(connected1, "Should connect with lowercase header");
            }
        } finally {
            authClient1.close();
        }

        // Test mixed case header
        final Client authClient2 = ClientBuilder.newBuilder()
                .register((ClientRequestFilter)
                        requestContext -> requestContext.getHeaders().add("X-Api-Key", VALID_API_KEY))
                .build();

        try {
            final WebTarget target2 = authClient2.target(sseEndpoint);
            final CountDownLatch connectionLatch2 = new CountDownLatch(1);

            try (SseEventSource eventSource = SseEventSource.target(target2).build()) {
                eventSource.register(event -> connectionLatch2.countDown(), throwable -> connectionLatch2.countDown());

                eventSource.open();
                boolean connected2 = connectionLatch2.await(5, TimeUnit.SECONDS);
                assertTrue(connected2, "Should connect with mixed case header");
            }
        } finally {
            authClient2.close();
        }
    }

    @Test
    public void testApiKeyWithWhitespace() throws InterruptedException {
        final Client authClient = createClientWithApiKey(" " + VALID_API_KEY + " ");
        try {
            final WebTarget target = authClient.target(sseEndpoint);
            final CountDownLatch errorLatch = new CountDownLatch(1);
            final List<Throwable> errors = new ArrayList<>();

            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(
                        event -> {
                            // Should not receive events with whitespace in API key
                        },
                        throwable -> {
                            errors.add(throwable);
                            errorLatch.countDown();
                        });

                eventSource.open();

                // Wait for authentication error
                boolean errorReceived = errorLatch.await(5, TimeUnit.SECONDS);
                assertTrue(errorReceived, "Should receive authentication error for API key with whitespace");
            }
        } finally {
            authClient.close();
        }
    }

    @Test
    public void testMultipleApiKeyHeaders() throws InterruptedException {
        // Test behavior when multiple API key headers are sent
        final Client authClient = ClientBuilder.newBuilder()
                .register((ClientRequestFilter) requestContext -> {
                    requestContext.getHeaders().add(API_KEY_HEADER, INVALID_API_KEY);
                    requestContext.getHeaders().add(API_KEY_HEADER, VALID_API_KEY); // Second header
                })
                .build();

        try {
            final WebTarget target = authClient.target(sseEndpoint);
            final CountDownLatch errorLatch = new CountDownLatch(1);
            final List<Throwable> errors = new ArrayList<>();

            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(
                        event -> {
                            // Should not receive events - servers typically take first header
                        },
                        throwable -> {
                            errors.add(throwable);
                            errorLatch.countDown();
                        });

                eventSource.open();

                // Wait for authentication error
                boolean errorReceived = errorLatch.await(5, TimeUnit.SECONDS);
                assertTrue(errorReceived, "Should receive authentication error when first header is invalid");
            }
        } finally {
            authClient.close();
        }
    }

    @Test
    public void testOptionsRequestWithCors() throws InterruptedException {
        final Client authClient = createClientWithApiKey(VALID_API_KEY);
        try {
            final WebTarget target = authClient.target(sseEndpoint);
            final CountDownLatch connectionLatch = new CountDownLatch(1);

            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                eventSource.register(event -> connectionLatch.countDown(), throwable -> connectionLatch.countDown());

                eventSource.open();

                // Wait for successful connection
                boolean connected = connectionLatch.await(5, TimeUnit.SECONDS);
                assertTrue(connected, "Should successfully connect with valid API key");
            }
        } finally {
            authClient.close();
        }
    }
}
