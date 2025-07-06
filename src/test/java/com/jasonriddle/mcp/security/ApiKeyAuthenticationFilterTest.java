package com.jasonriddle.mcp.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for API key authentication filter.
 */
@QuarkusTest
final class ApiKeyAuthenticationFilterTest {

    private ApiKeyAuthenticationFilter filter;
    private TestContainerRequestContext requestContext;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter();
        requestContext = new TestContainerRequestContext();
    }

    @Test
    void testSecurityDisabled() throws IOException {
        filter.securityEnabled = false;
        filter.expectedApiKey = Optional.of("test-key");

        filter.filter(requestContext);

        assertFalse(requestContext.isAborted());
    }

    @Test
    void testEmptyApiKey() throws IOException {
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.empty();

        filter.filter(requestContext);

        assertFalse(requestContext.isAborted());
    }

    @Test
    void testValidApiKey() throws IOException {
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.of("valid-test-key");
        requestContext.setHeaderString("X-API-Key", "valid-test-key");

        filter.filter(requestContext);

        assertFalse(requestContext.isAborted());
    }

    @Test
    void testMissingApiKey() throws IOException {
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.of("test-key");
        requestContext.setHeaderString("X-API-Key", null);

        filter.filter(requestContext);

        assertTrue(requestContext.isAborted());
        assertTrue(requestContext.getResponse().getStatus() == 401);
    }

    @Test
    void testInvalidApiKey() throws IOException {
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.of("valid-key");
        requestContext.setHeaderString("X-API-Key", "invalid-key");

        filter.filter(requestContext);

        assertTrue(requestContext.isAborted());
        assertTrue(requestContext.getResponse().getStatus() == 401);
    }

    @Test
    void testNullApiKeyString() throws IOException {
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.of("valid-key");
        requestContext.setHeaderString("X-API-Key", null);

        filter.filter(requestContext);

        assertTrue(requestContext.isAborted());
        assertTrue(requestContext.getResponse().getStatus() == 401);
    }

    @Test
    void testEmptyStringApiKey() throws IOException {
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.of("valid-key");
        requestContext.setHeaderString("X-API-Key", "");

        filter.filter(requestContext);

        assertTrue(requestContext.isAborted());
        assertTrue(requestContext.getResponse().getStatus() == 401);
    }

    @Test
    void testWhitespaceApiKey() throws IOException {
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.of("valid-key");
        requestContext.setHeaderString("X-API-Key", "   ");

        filter.filter(requestContext);

        assertTrue(requestContext.isAborted());
        assertTrue(requestContext.getResponse().getStatus() == 401);
    }
}
