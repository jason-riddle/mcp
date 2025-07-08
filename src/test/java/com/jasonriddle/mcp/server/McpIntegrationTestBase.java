package com.jasonriddle.mcp.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for MCP integration tests providing common functionality.
 *
 * This class contains shared test logic, utilities, and data that are used
 * across both SSE and STDIO integration tests to eliminate code duplication.
 */
abstract class McpIntegrationTestBase {

    protected static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(10);
    protected static final String SSE_TEST_MEMORY_FILE = "memory-sse-int-test.jsonl";
    protected static final String STDIO_TEST_MEMORY_FILE = "memory-stdio-int-test.jsonl";

    protected McpClient mcpClient;
    protected ObjectMapper objectMapper;

    /**
     * Returns the memory file path for the specific test implementation.
     */
    protected abstract String getTestMemoryFile();

    /**
     * Sets up the MCP client for the specific transport type.
     */
    protected abstract void setupMcpClient() throws Exception;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        cleanupTestMemoryFile();
        setupMcpClient();
        waitForClientReady();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mcpClient != null) {
            try {
                mcpClient.close();
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                // Expected during shutdown, ignore
            }
        }
        cleanupTestMemoryFile();
    }

    /**
     * Tests basic MCP connection and graph reading functionality.
     */
    protected void testBasicConnection() throws Exception {
        assertNotNull(mcpClient);

        String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_read_graph")
                .arguments("{}")
                .build());

        assertNotNull(result);
        assertTrue(result.length() > 0);

        JsonNode jsonNode = objectMapper.readTree(result);
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("entities"));
        assertTrue(jsonNode.has("relations"));
    }

    /**
     * Tests creating entities and reading the graph.
     */
    protected void testCreateEntitiesAndReadGraph(final String entityName) throws Exception {
        String createEntitiesJson = objectMapper.writeValueAsString(Map.of(
                "entities",
                List.of(Map.of(
                        "name",
                        entityName,
                        "entityType",
                        "TestType",
                        "observations",
                        List.of("This is a test entity for integration testing")))));

        String createResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_create_entities")
                .arguments(createEntitiesJson)
                .build());

        assertNotNull(createResult);
        assertTrue(createResult.length() > 0);

        String readResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_read_graph")
                .arguments("{}")
                .build());

        assertNotNull(readResult);
        assertTrue(readResult.length() > 0);

        JsonNode graphNode = objectMapper.readTree(readResult);
        JsonNode entities = graphNode.get("entities");
        boolean foundTestEntity = false;
        for (JsonNode entity : entities) {
            if (entityName.equals(entity.get("name").asText())) {
                foundTestEntity = true;
                break;
            }
        }
        assertTrue(foundTestEntity, entityName + " should be found in memory graph");
    }

    /**
     * Tests searching for nodes in the memory graph.
     */
    protected void testSearchNodes(final String searchTerm, final String expectedEntityName) throws Exception {
        String searchArgsJson = objectMapper.writeValueAsString(Map.of("query", searchTerm));

        String searchResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_search_nodes")
                .arguments(searchArgsJson)
                .build());

        assertNotNull(searchResult);
        assertTrue(searchResult.length() > 0);

        JsonNode searchNode = objectMapper.readTree(searchResult);
        assertNotNull(searchNode);
        assertTrue(searchNode.has("entities"));
        assertTrue(searchNode.has("relations"));

        JsonNode entities = searchNode.get("entities");
        assertTrue(entities.isArray());
        boolean foundSearchEntity = false;
        for (JsonNode entity : entities) {
            if (expectedEntityName.equals(entity.get("name").asText())) {
                foundSearchEntity = true;
                break;
            }
        }
        assertTrue(foundSearchEntity, "Search should find the " + expectedEntityName);
    }

    /**
     * Tests prompt discovery and retrieval functionality.
     */
    protected void testPromptDiscovery() throws Exception {
        try {
            var prompts = mcpClient.listPrompts();

            assertNotNull(prompts, "Prompts list should not be null");
            assertFalse(prompts.isEmpty(), "Should have at least one prompt registered");

            boolean foundMemoryPrompt =
                    prompts.stream().anyMatch(prompt -> "memory_best_practices".equals(prompt.name()));

            assertTrue(foundMemoryPrompt, "Should find memory_best_practices prompt");

            var promptArgs = Map.<String, Object>of();
            var promptResult = mcpClient.getPrompt("memory_best_practices", promptArgs);
            assertNotNull(promptResult, "Should be able to get prompt content");
            assertFalse(promptResult.messages().isEmpty(), "Prompt should have message content");

        } catch (Exception e) {
            throw new AssertionError("Prompts not discovered by MCP server: " + e.getMessage(), e);
        }
    }

    /**
     * Tests resource discovery and retrieval functionality.
     */
    protected void testResourceDiscovery() throws Exception {
        try {
            var resources = mcpClient.listResources();

            assertNotNull(resources, "Resources list should not be null");
            assertFalse(resources.isEmpty(), "Should have at least one resource registered");

            boolean foundTypesResource =
                    resources.stream().anyMatch(resource -> "memory://types".equals(resource.uri()));
            boolean foundStatusResource =
                    resources.stream().anyMatch(resource -> "memory://status".equals(resource.uri()));

            assertTrue(foundTypesResource, "Should find memory://types resource");
            assertTrue(foundStatusResource, "Should find memory://status resource");

            var typesResult = mcpClient.readResource("memory://types");
            assertNotNull(typesResult, "Should be able to read types resource");
            assertFalse(typesResult.contents().isEmpty(), "Types resource should have content");

        } catch (Exception e) {
            throw new AssertionError("Resources not discovered by MCP server: " + e.getMessage(), e);
        }
    }

    /**
     * Waits for the MCP client to be ready by retrying a simple tool call.
     */
    protected void waitForClientReady() throws Exception {
        final int maxRetries = 5;
        final long retryDelayMs = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                        .name("memory_read_graph")
                        .arguments("{}")
                        .build());

                if (result != null && result.length() > 0) {
                    return;
                }
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("MCP client failed to initialize after " + maxRetries + " attempts", e);
                }
                Thread.sleep(retryDelayMs);
            }
        }
    }

    /**
     * Cleans up the test memory file to ensure test isolation.
     */
    protected void cleanupTestMemoryFile() {
        try {
            Path memoryFile = Paths.get(getTestMemoryFile());
            Files.deleteIfExists(memoryFile);
        } catch (IOException e) {
            // Ignore cleanup failures
        }
    }
}
