package com.jasonriddle.mcp.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Simple API key authentication filter for MCP server. */
@Provider
public class ApiKeyAuthenticationFilter implements ContainerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @ConfigProperty(name = "mcp.security.api-key")
    Optional<String> expectedApiKey;

    @ConfigProperty(name = "mcp.security.enabled", defaultValue = "false")
    boolean securityEnabled;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        // Skip authentication if security is disabled or no API key configured
        if (!securityEnabled || expectedApiKey.isEmpty()) {
            return;
        }

        // Get API key from header
        final String providedApiKey = requestContext.getHeaderString(API_KEY_HEADER);

        // Validate API key
        if (providedApiKey == null || !providedApiKey.equals(expectedApiKey.get())) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid or missing API key")
                    .build());
        }
    }
}
