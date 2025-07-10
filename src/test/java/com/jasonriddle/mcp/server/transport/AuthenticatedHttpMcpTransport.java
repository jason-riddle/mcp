package com.jasonriddle.mcp.server.transport;

// Authenticated HTTP MCP transport factory with Bearer token support

import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Factory for creating authenticated HTTP MCP transports with Bearer token support.
 *
 * This factory creates HttpMcpTransport instances and uses reflection to inject
 * a custom OkHttp client that automatically adds Authorization header with Bearer token.
 */
public final class AuthenticatedHttpMcpTransport {

    private AuthenticatedHttpMcpTransport() {
        // Utility class
    }

    /**
     * Creates an authenticated HTTP MCP transport with Bearer token using reflection.
     *
     * @param sseUrl SSE endpoint URL
     * @param bearerToken Bearer token for authentication
     * @param timeout request timeout
     * @param logRequests enable request logging
     * @param logResponses enable response logging
     * @return configured McpTransport with authentication
     */
    public static McpTransport create(
            final String sseUrl,
            final String bearerToken,
            final Duration timeout,
            final boolean logRequests,
            final boolean logResponses) {

        if (sseUrl == null || sseUrl.isEmpty()) {
            throw new IllegalArgumentException("SSE URL is required");
        }
        if (bearerToken == null || bearerToken.isEmpty()) {
            throw new IllegalArgumentException("Bearer token is required");
        }

        // Create the HttpMcpTransport
        HttpMcpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(sseUrl)
                .timeout(timeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();

        try {
            // Create authenticated OkHttp client
            BearerTokenInterceptor tokenInterceptor = new BearerTokenInterceptor(bearerToken);
            OkHttpClient authenticatedClient = new OkHttpClient.Builder()
                    .connectTimeout(timeout)
                    .readTimeout(timeout)
                    .writeTimeout(timeout)
                    .addInterceptor(tokenInterceptor)
                    .build();

            // Use reflection to inject the authenticated client
            Field clientField = HttpMcpTransport.class.getDeclaredField("client");
            clientField.setAccessible(true);
            clientField.set(transport, authenticatedClient);

        } catch (Exception e) {
            throw new RuntimeException("Failed to inject authenticated HTTP client", e);
        }

        return transport;
    }

    /**
     * Builder for creating authenticated HTTP MCP transports.
     */
    public static final class Builder {
        private String sseUrl;
        private String bearerToken;
        private Duration timeout = Duration.ofSeconds(10);
        private boolean logRequests = false;
        private boolean logResponses = false;

        /**
         * Sets the SSE endpoint URL.
         *
         * @param url SSE endpoint URL
         * @return this builder
         */
        public Builder sseUrl(final String url) {
            this.sseUrl = url;
            return this;
        }

        /**
         * Sets the Bearer token for authentication.
         *
         * @param token Bearer token value
         * @return this builder
         */
        public Builder bearerToken(final String token) {
            this.bearerToken = token;
            return this;
        }

        /**
         * Sets the timeout for HTTP operations.
         *
         * @param requestTimeout timeout duration
         * @return this builder
         */
        public Builder timeout(final Duration requestTimeout) {
            this.timeout = requestTimeout;
            return this;
        }

        /**
         * Enables or disables request logging.
         *
         * @param enableRequestLogging true to enable request logging
         * @return this builder
         */
        public Builder logRequests(final boolean enableRequestLogging) {
            this.logRequests = enableRequestLogging;
            return this;
        }

        /**
         * Enables or disables response logging.
         *
         * @param enableResponseLogging true to enable response logging
         * @return this builder
         */
        public Builder logResponses(final boolean enableResponseLogging) {
            this.logResponses = enableResponseLogging;
            return this;
        }

        /**
         * Builds the authenticated McpTransport instance.
         *
         * @return configured transport instance
         */
        public McpTransport build() {
            return create(sseUrl, bearerToken, timeout, logRequests, logResponses);
        }
    }

    /**
     * OkHttp interceptor that adds Bearer token authentication to all requests.
     */
    private static final class BearerTokenInterceptor implements Interceptor {
        private final String bearerToken;

        BearerTokenInterceptor(final String bearerToken) {
            this.bearerToken = bearerToken;
        }

        @Override
        public Response intercept(final Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request authenticatedRequest = originalRequest
                    .newBuilder()
                    .header("Authorization", "Bearer " + bearerToken)
                    .build();
            return chain.proceed(authenticatedRequest);
        }
    }
}
