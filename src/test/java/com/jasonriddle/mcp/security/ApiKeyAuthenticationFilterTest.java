package com.jasonriddle.mcp.security;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Tests for API key authentication filter. */
@QuarkusTest
public class ApiKeyAuthenticationFilterTest {

    private ApiKeyAuthenticationFilter filter;
    private ContainerRequestContext requestContext;

    @BeforeEach
    public void setUp() {
        filter = new ApiKeyAuthenticationFilter();
        requestContext = mock(ContainerRequestContext.class);
    }

    @Test
    public void testSecurityDisabled() throws IOException {
        // Given security is disabled
        filter.securityEnabled = false;
        filter.expectedApiKey = Optional.of("test-key");

        // When filter is applied
        filter.filter(requestContext);

        // Then request should not be aborted
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    public void testEmptyApiKey() throws IOException {
        // Given security is enabled but API key is empty
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.empty();

        // When filter is applied
        filter.filter(requestContext);

        // Then request should not be aborted
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    public void testValidApiKey() throws IOException {
        // Given security is enabled with valid API key
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.of("valid-test-key");
        when(requestContext.getHeaderString("X-API-Key")).thenReturn("valid-test-key");

        // When filter is applied
        filter.filter(requestContext);

        // Then request should not be aborted
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    public void testMissingApiKey() throws IOException {
        // Given security is enabled but no API key provided
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.of("test-key");
        when(requestContext.getHeaderString("X-API-Key")).thenReturn(null);

        // When filter is applied
        filter.filter(requestContext);

        // Then request should be aborted with 401
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        Response response = responseCaptor.getValue();
        assert response.getStatus() == 401;
    }

    @Test
    public void testInvalidApiKey() throws IOException {
        // Given security is enabled with invalid API key
        filter.securityEnabled = true;
        filter.expectedApiKey = Optional.of("valid-key");
        when(requestContext.getHeaderString("X-API-Key")).thenReturn("invalid-key");

        // When filter is applied
        filter.filter(requestContext);

        // Then request should be aborted with 401
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        Response response = responseCaptor.getValue();
        assert response.getStatus() == 401;
    }
}
