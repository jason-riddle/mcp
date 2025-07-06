package com.jasonriddle.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
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
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for MCP Server STDIO transport.
 *
 * This test suite verifies that the MCP server can be launched as a subprocess
 * and communicates properly via standard input/output streams using the
 * Model Context Protocol.
 */
@QuarkusIntegrationTest
@TestProfile(McpServerStdioIntegrationTest.TestProfile.class)
@TestMethodOrder(OrderAnnotation.class)
final class McpServerStdioIntegrationTest {

    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(10);
    private static final String TEST_MEMORY_FILE = "test-memory-stdio.jsonl";

    private McpClient mcpClient;
    private ObjectMapper objectMapper;

    /**
     * Test profile configuration for MCP STDIO integration tests.
     */
    public static final class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    // Use separate memory file for integration tests
                    "memory.file.path", TEST_MEMORY_FILE,
                    // Reduce log noise during testing
                    "quarkus.log.level", "WARN",
                    "quarkus.log.category.\"com.jasonriddle.mcp\"", "INFO");
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();

        // Clean up any existing test memory file
        cleanupTestMemoryFile();

        // Determine the path to the packaged JAR
        String jarPath = getQuarkusJarPath();

        // Configure STDIO transport to spawn MCP server subprocess
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of("java", "-jar", jarPath))
                .logEvents(true) // Enable logging for debugging
                .build();

        // Create MCP client with configured transport
        mcpClient = new DefaultMcpClient.Builder()
                .clientName("stdio-integration-test-client")
                .toolExecutionTimeout(CLIENT_TIMEOUT)
                .transport(transport)
                .build();

        // Wait for client to be properly initialized
        // The LangChain4j alpha version has a known issue where it doesn't automatically send
        // the "initialized" notification after the initialize response. We need to wait longer
        // and potentially retry to allow the server to be ready for tool calls.
        Thread.sleep(3000); // Give the client time to complete initialization

        // Verify the client is ready by attempting a simple tool call with retry logic
        waitForClientReady();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mcpClient != null) {
            try {
                mcpClient.close();
                // Give the subprocess time to terminate gracefully
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                // Expected during shutdown, ignore
            }
        }

        // Clean up test memory file
        cleanupTestMemoryFile();
    }

    @Test
    @Order(1)
    void shouldInitializeMcpConnection() throws Exception {
        // The client should successfully initialize the MCP connection
        // This happens automatically when the client is created and verified in waitForClientReady()
        assertNotNull(mcpClient);

        // Execute a tool call to verify initialization (client is already ready)
        String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_read_graph")
                .arguments("{}")
                .build());

        assertNotNull(result);
        assertTrue(result.length() > 0);

        // Should be valid JSON containing entities and relations
        JsonNode jsonNode = objectMapper.readTree(result);
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("entities"));
        assertTrue(jsonNode.has("relations"));
    }

    @Test
    @Order(2)
    void shouldExecuteCreateEntitiesAndReadGraph() throws Exception {
        // Create test entities using JSON string arguments
        String createEntitiesJson = objectMapper.writeValueAsString(Map.of(
                "entities",
                List.of(Map.of(
                        "name", "STDIOTestEntity",
                        "entityType", "TestType",
                        "observations", List.of("This is a test entity for STDIO integration testing")))));

        String createResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_create_entities")
                .arguments(createEntitiesJson)
                .build());

        assertNotNull(createResult);
        assertTrue(createResult.length() > 0);

        // Read the graph to verify the entity was created
        String readResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_read_graph")
                .arguments("{}")
                .build());

        assertNotNull(readResult);
        assertTrue(readResult.length() > 0);

        // Verify the test entity is present in the graph
        JsonNode graphNode = objectMapper.readTree(readResult);
        JsonNode entities = graphNode.get("entities");
        boolean foundTestEntity = false;
        for (JsonNode entity : entities) {
            if ("STDIOTestEntity".equals(entity.get("name").asText())) {
                foundTestEntity = true;
                break;
            }
        }
        assertTrue(foundTestEntity, "STDIOTestEntity should be found in memory graph");
    }

    @Test
    @Order(3)
    void shouldExecuteSearchNodesTool() throws Exception {
        // Search for nodes using a query
        String searchArgsJson = objectMapper.writeValueAsString(Map.of("query", "STDIO"));

        String searchResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_search_nodes")
                .arguments(searchArgsJson)
                .build());

        assertNotNull(searchResult);
        assertTrue(searchResult.length() > 0);

        // Verify search results are valid MemoryGraph JSON object (not array)
        JsonNode searchNode = objectMapper.readTree(searchResult);
        assertNotNull(searchNode);
        assertTrue(searchNode.has("entities"));
        assertTrue(searchNode.has("relations"));

        // Verify the entities array contains our STDIO test entity
        JsonNode entities = searchNode.get("entities");
        assertTrue(entities.isArray());
        boolean foundSTDIOEntity = false;
        for (JsonNode entity : entities) {
            if ("STDIOTestEntity".equals(entity.get("name").asText())) {
                foundSTDIOEntity = true;
                break;
            }
        }
        assertTrue(foundSTDIOEntity, "Search should find the STDIOTestEntity");
    }

    @Test
    @Order(4)
    void shouldHandleSubprocessTermination() throws Exception {
        // This test verifies that we can properly manage the subprocess lifecycle

        // Execute a tool to ensure connection is working
        String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_read_graph")
                .arguments("{}")
                .build());

        assertNotNull(result);

        // Close the client, which should terminate the subprocess
        mcpClient.close();

        // Verify we can recreate the connection successfully
        String jarPath = getQuarkusJarPath();
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of("java", "-jar", jarPath))
                .logEvents(true)
                .build();

        mcpClient = new DefaultMcpClient.Builder()
                .clientName("stdio-integration-test-client-2")
                .toolExecutionTimeout(CLIENT_TIMEOUT)
                .transport(transport)
                .build();

        // Wait for the new client to be properly initialized
        Thread.sleep(3000);

        // Verify the client is ready with retry logic
        waitForClientReady();

        // Verify the new connection works
        String newResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_read_graph")
                .arguments("{}")
                .build());

        assertNotNull(newResult);
    }

    @Test
    @Order(5)
    void shouldExecuteFullEntityCrudLifecycle() throws Exception {
        // This test covers the complete CRUD lifecycle:
        // A) Create an entity
        // B) Verify the entity was added
        // C) Search for the entity and find it
        // D) Delete the entity
        // E) Search for the entity and get empty results

        final String testEntityName = "CrudTestEntity";
        final String testEntityType = "CrudTest";
        final String testObservation = "This entity tests the full CRUD lifecycle";

        // A) Create an entity
        String createEntitiesJson = objectMapper.writeValueAsString(Map.of(
                "entities",
                List.of(Map.of(
                        "name", testEntityName,
                        "entityType", testEntityType,
                        "observations", List.of(testObservation)))));

        String createResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_create_entities")
                .arguments(createEntitiesJson)
                .build());

        assertNotNull(createResult);
        assertTrue(createResult.length() > 0);

        // B) Verify the entity was added (don't check total count since previous tests may have created entities)
        String readResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_read_graph")
                .arguments("{}")
                .build());

        assertNotNull(readResult);
        JsonNode graphNode = objectMapper.readTree(readResult);
        JsonNode entities = graphNode.get("entities");

        // Find our test entity and verify its properties
        boolean foundTestEntity = false;
        for (JsonNode entity : entities) {
            if (testEntityName.equals(entity.get("name").asText())) {
                foundTestEntity = true;
                // Verify entity properties
                assertEquals(testEntityType, entity.get("entityType").asText());
                JsonNode observations = entity.get("observations");
                assertTrue(observations.isArray());
                assertEquals(1, observations.size());
                assertEquals(testObservation, observations.get(0).asText());
                break;
            }
        }

        assertTrue(foundTestEntity, "CrudTestEntity should be found in the graph");

        // C) Search for the entity and find it
        String searchArgsJson = objectMapper.writeValueAsString(Map.of("query", "CrudTest"));
        String searchResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_search_nodes")
                .arguments(searchArgsJson)
                .build());

        assertNotNull(searchResult);
        JsonNode searchGraph = objectMapper.readTree(searchResult);
        assertTrue(searchGraph.has("entities"));
        assertTrue(searchGraph.has("relations"));

        JsonNode searchEntities = searchGraph.get("entities");
        assertTrue(searchEntities.isArray());
        assertTrue(searchEntities.size() > 0, "Search should find the CrudTestEntity");

        // Verify the search result contains our entity
        boolean foundInSearch = false;
        for (JsonNode searchEntity : searchEntities) {
            if (testEntityName.equals(searchEntity.get("name").asText())) {
                foundInSearch = true;
                break;
            }
        }
        assertTrue(foundInSearch, "CrudTestEntity should be found in search results");

        // D) Delete the entity and assert size is 0
        String deleteEntitiesJson = objectMapper.writeValueAsString(Map.of("entityNames", List.of(testEntityName)));

        String deleteResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_delete_entities")
                .arguments(deleteEntitiesJson)
                .build());

        assertNotNull(deleteResult);
        assertTrue(deleteResult.length() > 0);

        // Verify the entity was deleted - read graph again
        String readAfterDeleteResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_read_graph")
                .arguments("{}")
                .build());

        assertNotNull(readAfterDeleteResult);
        JsonNode graphAfterDelete = objectMapper.readTree(readAfterDeleteResult);
        JsonNode entitiesAfterDelete = graphAfterDelete.get("entities");

        // Verify our specific entity was deleted (other entities from previous tests may still exist)
        boolean foundDeletedEntity = false;
        for (JsonNode entity : entitiesAfterDelete) {
            if (testEntityName.equals(entity.get("name").asText())) {
                foundDeletedEntity = true;
                break;
            }
        }

        assertFalse(foundDeletedEntity, "CrudTestEntity should not be found after deletion");

        // E) Search for the entity and get empty results
        String searchAfterDeleteResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_search_nodes")
                .arguments(searchArgsJson)
                .build());

        assertNotNull(searchAfterDeleteResult);
        JsonNode searchAfterDeleteGraph = objectMapper.readTree(searchAfterDeleteResult);
        assertTrue(searchAfterDeleteGraph.has("entities"));
        assertTrue(searchAfterDeleteGraph.has("relations"));

        JsonNode searchAfterDeleteEntities = searchAfterDeleteGraph.get("entities");
        assertTrue(searchAfterDeleteEntities.isArray());

        // Verify the search no longer finds the deleted entity
        boolean foundDeletedInSearch = false;
        for (JsonNode searchEntity : searchAfterDeleteEntities) {
            if (testEntityName.equals(searchEntity.get("name").asText())) {
                foundDeletedInSearch = true;
                break;
            }
        }
        assertFalse(foundDeletedInSearch, "CrudTestEntity should not be found in search after deletion");
    }

    /**
     * Determines the path to the packaged Quarkus JAR for subprocess execution.
     * This method looks for the standard Quarkus build output locations.
     */
    private String getQuarkusJarPath() throws IOException {
        // Try different possible JAR locations
        List<String> possiblePaths = List.of(
                "target/quarkus-app/quarkus-run.jar", // Standard Quarkus JAR
                "target/jasons-mcp-server-*-runner.jar" // Uber JAR if built
                );

        for (String pathPattern : possiblePaths) {
            if (pathPattern.contains("*")) {
                // Handle wildcard patterns for uber JARs
                Path targetDir = Paths.get("target");
                if (Files.exists(targetDir)) {
                    try (var files = Files.list(targetDir)) {
                        var matchingFile = files.filter(
                                        p -> p.getFileName().toString().contains("runner.jar"))
                                .findFirst();
                        if (matchingFile.isPresent()) {
                            return matchingFile.get().toAbsolutePath().toString();
                        }
                    }
                }
            } else {
                Path jarPath = Paths.get(pathPattern);
                if (Files.exists(jarPath)) {
                    return jarPath.toAbsolutePath().toString();
                }
            }
        }

        throw new IOException(
                "Could not find packaged Quarkus JAR. " + "Make sure to run 'mvn package' before integration tests.");
    }

    /**
     * Waits for the MCP client to be ready by retrying a simple tool call.
     * This is necessary due to a bug in LangChain4j alpha version where the
     * "initialized" notification is not automatically sent.
     */
    private void waitForClientReady() throws Exception {
        final int maxRetries = 5;
        final long retryDelayMs = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Try a simple tool call to verify the client is ready
                String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                        .name("memory_read_graph")
                        .arguments("{}")
                        .build());

                // If we get here without exception, the client is ready
                if (result != null && result.length() > 0) {
                    return;
                }
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("MCP client failed to initialize after " + maxRetries + " attempts", e);
                }
                // Wait before retrying
                Thread.sleep(retryDelayMs);
            }
        }
    }

    /**
     * Cleans up the test memory file to ensure test isolation.
     */
    private void cleanupTestMemoryFile() {
        try {
            Path memoryFile = Paths.get(TEST_MEMORY_FILE);
            Files.deleteIfExists(memoryFile);
        } catch (IOException e) {
            // Ignore cleanup failures
        }
    }
}
