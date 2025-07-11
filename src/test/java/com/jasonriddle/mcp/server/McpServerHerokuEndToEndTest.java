package com.jasonriddle.mcp.server;

// End-to-end tests for MCP Server remote endpoint functionality

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.jasonriddle.mcp.server.transport.AuthenticatedHttpMcpTransport;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.time.Duration;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for MCP Server remote Heroku endpoint.
 *
 * This test suite verifies that our MCP client can successfully communicate
 * with a remote MCP server hosted on Heroku using Bearer token authentication.
 * These are true end-to-end tests that validate the complete flow from client
 * to remote production service.
 */
@QuarkusTest
@TestProfile(McpServerHerokuEndToEndTest.RemoteTestProfile.class)
final class McpServerHerokuEndToEndTest extends McpIntegrationTestBase {

    private static final String REMOTE_SSE_URL = "https://us.inference.heroku.com/mcp/sse";
    private static final Duration REMOTE_TIMEOUT = Duration.ofSeconds(30);
    private static final String E2E_TEST_MEMORY_FILE = "memory-e2e-test.jsonl";

    @ConfigProperty(name = "heroku.mcp.token")
    String herokuMcpToken;

    /**
     * Test profile configuration for remote MCP end-to-end tests.
     */
    public static final class RemoteTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("memory.file.path", E2E_TEST_MEMORY_FILE);
        }

        @Override
        public String getConfigProfile() {
            return "heroku-end-to-end-test";
        }
    }

    @Override
    protected String getTestMemoryFile() {
        return E2E_TEST_MEMORY_FILE;
    }

    @Override
    protected void setupMcpClient() throws Exception {
        McpTransport transport = new AuthenticatedHttpMcpTransport.Builder()
                .sseUrl(REMOTE_SSE_URL)
                .bearerToken(herokuMcpToken)
                .timeout(REMOTE_TIMEOUT)
                .logRequests(true)
                .logResponses(true)
                .build();

        mcpClient = new DefaultMcpClient.Builder()
                .clientName("remote-e2e-test-client")
                .protocolVersion("2024-11-05")
                .toolExecutionTimeout(REMOTE_TIMEOUT)
                .transport(transport)
                .build();

        // Allow more time for remote connection establishment
        Thread.sleep(3000);
    }

    @Test
    void shouldEstablishRemoteConnection() throws Exception {
        assertNotNull(mcpClient);

        // First, let's see what tools are available
        var tools = mcpClient.listTools();
        assertNotNull(tools);
        assertFalse(tools.isEmpty());

        System.out.println("Available tools on remote server:");
        for (var tool : tools) {
            System.out.println("- " + tool.name() + ": " + tool.description());
        }

        // Test with memory.read_graph which doesn't require arguments
        String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("mcp-memory/memory.read_graph")
                .arguments("{}")
                .build());

        assertNotNull(result);
        assertTrue(result.length() > 0);

        JsonNode jsonNode = objectMapper.readTree(result);
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("entities"));
        assertTrue(jsonNode.has("relations"));
        System.out.println("Successfully executed remote memory.read_graph!");
    }

    // NOTE: Additional tests commented out as the remote Heroku MCP server
    // may have different tool availability than our local implementation.
    // The main test above validates that authentication and basic connectivity work.
}
