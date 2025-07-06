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
                    "quarkus.log.category.\"com.jasonriddle.mcp\"", "INFO",
                    // Ensure STDIO transport is enabled
                    "quarkus.mcp.server.stdio.enabled", "true",
                    "quarkus.mcp.server.stdio.initialization-enabled", "true");
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
        // This happens automatically when the client is created
        assertNotNull(mcpClient);

        // Verify initialization by executing a simple tool call
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

        // Verify search results are valid JSON array
        JsonNode searchNode = objectMapper.readTree(searchResult);
        assertNotNull(searchNode);
        assertTrue(searchNode.isArray());
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
        // B) Verify the entity was added and count is 1
        // C) Search for the entity and find it
        // D) Delete the entity and assert size is 0
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

        // B) Verify the entity was added and count of entities is 1
        String readResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_read_graph")
                .arguments("{}")
                .build());

        assertNotNull(readResult);
        JsonNode graphNode = objectMapper.readTree(readResult);
        JsonNode entities = graphNode.get("entities");

        // Count entities and verify our test entity exists
        int entityCount = 0;
        boolean foundTestEntity = false;
        for (JsonNode entity : entities) {
            entityCount++;
            if (testEntityName.equals(entity.get("name").asText())) {
                foundTestEntity = true;
                // Verify entity properties
                assertEquals(testEntityType, entity.get("entityType").asText());
                JsonNode observations = entity.get("observations");
                assertTrue(observations.isArray());
                assertEquals(1, observations.size());
                assertEquals(testObservation, observations.get(0).asText());
            }
        }

        assertEquals(1, entityCount, "Expected exactly 1 entity in the graph");
        assertTrue(foundTestEntity, "CrudTestEntity should be found in the graph");

        // C) Search for the entity and find it
        String searchArgsJson = objectMapper.writeValueAsString(Map.of("query", "CrudTest"));
        String searchResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_search_nodes")
                .arguments(searchArgsJson)
                .build());

        assertNotNull(searchResult);
        JsonNode searchNodes = objectMapper.readTree(searchResult);
        assertTrue(searchNodes.isArray());
        assertTrue(searchNodes.size() > 0, "Search should find the CrudTestEntity");

        // Verify the search result contains our entity
        boolean foundInSearch = false;
        for (JsonNode searchNode : searchNodes) {
            if (testEntityName.equals(searchNode.get("name").asText())) {
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

        // Count entities after deletion
        int entityCountAfterDelete = 0;
        boolean foundDeletedEntity = false;
        for (JsonNode entity : entitiesAfterDelete) {
            entityCountAfterDelete++;
            if (testEntityName.equals(entity.get("name").asText())) {
                foundDeletedEntity = true;
            }
        }

        assertEquals(0, entityCountAfterDelete, "Expected 0 entities after deletion");
        assertFalse(foundDeletedEntity, "CrudTestEntity should not be found after deletion");

        // E) Search for the entity and get empty results
        String searchAfterDeleteResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                .name("memory_search_nodes")
                .arguments(searchArgsJson)
                .build());

        assertNotNull(searchAfterDeleteResult);
        JsonNode searchAfterDelete = objectMapper.readTree(searchAfterDeleteResult);
        assertTrue(searchAfterDelete.isArray());

        // Verify the search no longer finds the deleted entity
        boolean foundDeletedInSearch = false;
        for (JsonNode searchNode : searchAfterDelete) {
            if (testEntityName.equals(searchNode.get("name").asText())) {
                foundDeletedInSearch = true;
                break;
            }
        }
        assertFalse(foundDeletedInSearch, "CrudTestEntity should not be found in search after deletion");

        // The search result should either be empty or not contain our entity
        // (it might contain other entities if any exist from previous tests)
        assertTrue(
                searchAfterDelete.size() == 0 || !foundDeletedInSearch,
                "Search should either be empty or not contain the deleted entity");
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
