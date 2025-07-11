package com.jasonriddle.mcp.server;

// SSE integration tests for MCP server HTTP/SSE transport functionality

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for MCP Server SSE/HTTP transport.
 *
 * This test suite verifies that the MCP server properly handles SSE connections
 * and communicates using the Model Context Protocol over HTTP/SSE transport.
 */
@QuarkusTest
@TestProfile(McpServerSseIntegrationTest.TestProfile.class)
final class McpServerSseIntegrationTest extends McpIntegrationTestBase {

    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(10);
    private static final String SSE_TEST_MEMORY_FILE = "memory-sse-int-test.jsonl";

    @TestHTTPResource("/mcp/sse")
    URI sseEndpoint;

    @TestHTTPResource("/mcp")
    URI httpEndpoint;

    /**
     * Test profile configuration for MCP SSE integration tests.
     */
    public static final class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("memory.file.path", SSE_TEST_MEMORY_FILE);
        }

        @Override
        public String getConfigProfile() {
            return "sse-integration-test";
        }
    }

    @Override
    protected String getTestMemoryFile() {
        return SSE_TEST_MEMORY_FILE;
    }

    @Override
    protected void setupMcpClient() throws Exception {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(sseEndpoint.toString())
                .timeout(CLIENT_TIMEOUT)
                .build();

        mcpClient = new DefaultMcpClient.Builder()
                .clientName("sse-integration-test-client")
                .protocolVersion("2024-11-05")
                .toolExecutionTimeout(CLIENT_TIMEOUT)
                .transport(transport)
                .build();

        Thread.sleep(2000);
    }

    @Test
    void shouldEstablishSseConnection() throws Exception {
        assertNotNull(mcpClient);

        String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory.read_graph")
                .arguments("{}")
                .build());

        assertNotNull(result);
        assertTrue(result.length() > 0);

        JsonNode jsonNode = objectMapper.readTree(result);
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("entities"));
        assertTrue(jsonNode.has("relations"));
    }

    @Test
    void shouldCreateEntityAndReadGraph() throws Exception {
        String createEntitiesJson = objectMapper.writeValueAsString(Map.of(
                "entities",
                List.of(Map.of(
                        "name", "SSETestEntity",
                        "entityType", "TestType",
                        "observations", List.of("This is a test entity")))));

        String createResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory.create_entities")
                .arguments(createEntitiesJson)
                .build());

        assertNotNull(createResult);
        assertTrue(createResult.length() > 0);

        String readResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory.read_graph")
                .arguments("{}")
                .build());

        assertNotNull(readResult);
        JsonNode graphNode = objectMapper.readTree(readResult);
        JsonNode entities = graphNode.get("entities");

        boolean foundEntity = false;
        for (JsonNode entity : entities) {
            if ("SSETestEntity".equals(entity.get("name").asText())) {
                foundEntity = true;
                break;
            }
        }
        assertTrue(foundEntity, "SSETestEntity should be found in memory graph");
    }

    @Test
    void shouldSearchNodes() throws Exception {
        String searchArgsJson = objectMapper.writeValueAsString(Map.of("query", "test"));

        String searchResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory.search_nodes")
                .arguments(searchArgsJson)
                .build());

        assertNotNull(searchResult);
        JsonNode searchNode = objectMapper.readTree(searchResult);
        assertTrue(searchNode.has("entities"));
        assertTrue(searchNode.has("relations"));
    }

    @Test
    void shouldDiscoverPrompts() throws Exception {
        var prompts = mcpClient.listPrompts();
        assertNotNull(prompts);
        assertFalse(prompts.isEmpty());

        boolean foundMemoryPrompt = false;
        for (var prompt : prompts) {
            if ("memory.best_practices".equals(prompt.name())) {
                foundMemoryPrompt = true;
                break;
            }
        }
        assertTrue(foundMemoryPrompt, "Should find memory.best_practices prompt");
    }

    @Test
    void shouldDiscoverResources() throws Exception {
        var resources = mcpClient.listResources();
        assertNotNull(resources);
        assertFalse(resources.isEmpty());

        boolean foundTypesResource = false;
        for (var resource : resources) {
            if ("memory://types".equals(resource.uri())) {
                foundTypesResource = true;
                break;
            }
        }
        assertTrue(foundTypesResource, "Should find memory://types resource");
    }

    @Test
    void shouldExecuteTimeTools() throws Exception {
        String timeArgsJson = objectMapper.writeValueAsString(Map.of("timezone", "America/New_York"));

        String timeResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("time.get_current_time")
                .arguments(timeArgsJson)
                .build());

        assertNotNull(timeResult);
        JsonNode timeNode = objectMapper.readTree(timeResult);
        assertTrue(timeNode.has("timezone"));
        assertTrue(timeNode.has("datetime"));
    }
}
