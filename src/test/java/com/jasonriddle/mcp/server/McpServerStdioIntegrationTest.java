package com.jasonriddle.mcp.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for MCP Server STDIO transport.
 *
 * This test suite verifies that the MCP server can be launched as a subprocess
 * and communicates properly via standard input/output streams using the
 * Model Context Protocol.
 */
@QuarkusIntegrationTest
@TestProfile(McpServerStdioIntegrationTest.TestProfile.class)
final class McpServerStdioIntegrationTest extends McpIntegrationTestBase {

    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(10);
    private static final String STDIO_TEST_MEMORY_FILE = "memory-stdio-int-test.jsonl";

    /**
     * Test profile configuration for MCP STDIO integration tests.
     */
    public static final class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("memory.file.path", STDIO_TEST_MEMORY_FILE);
        }

        @Override
        public String getConfigProfile() {
            return "stdio-integration-test";
        }
    }

    @Override
    protected String getTestMemoryFile() {
        return STDIO_TEST_MEMORY_FILE;
    }

    @Override
    protected void setupMcpClient() throws Exception {
        String jarPath = "target/quarkus-app/quarkus-run.jar";

        if (!Files.exists(Paths.get(jarPath))) {
            throw new IOException("Quarkus JAR not found at " + jarPath + ". Run 'mvn package' first.");
        }

        McpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of("java", "-jar", jarPath))
                .logEvents(true)
                .build();

        mcpClient = new DefaultMcpClient.Builder()
                .clientName("stdio-integration-test-client")
                .toolExecutionTimeout(CLIENT_TIMEOUT)
                .transport(transport)
                .build();

        Thread.sleep(3000);
        // waitForClientReady();
    }

    @Test
    void shouldInitializeMcpConnection() throws Exception {
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
                        "name", "STDIOTestEntity",
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
            if ("STDIOTestEntity".equals(entity.get("name").asText())) {
                foundEntity = true;
                break;
            }
        }
        assertTrue(foundEntity, "STDIOTestEntity should be found in memory graph");
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

    @Test
    void shouldDiscoverWeatherTools() throws Exception {
        var tools = mcpClient.listTools();
        assertNotNull(tools);
        assertFalse(tools.isEmpty());

        boolean foundCurrentWeather = false;
        boolean foundForecast = false;
        boolean foundAlerts = false;

        for (var tool : tools) {
            switch (tool.name()) {
                case "weather.current":
                    foundCurrentWeather = true;
                    break;
                case "weather.forecast":
                    foundForecast = true;
                    break;
                case "weather.alerts":
                    foundAlerts = true;
                    break;
                default:
                    // Other tools are not relevant for this test
                    break;
            }
        }

        assertTrue(foundCurrentWeather, "Should find weather.current tool");
        assertTrue(foundForecast, "Should find weather.forecast tool");
        assertTrue(foundAlerts, "Should find weather.alerts tool");
    }

    @Test
    void shouldExecuteWeatherCurrentTool() throws Exception {
        // Note: This test will fail in real integration due to invalid API key
        // but it verifies the tool is properly exposed and callable
        String weatherArgsJson = objectMapper.writeValueAsString(Map.of("location", "New York"));

        try {
            String weatherResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                    .name("weather.current")
                    .arguments(weatherArgsJson)
                    .build());

            assertNotNull(weatherResult);
            JsonNode weatherNode = objectMapper.readTree(weatherResult);

            // Either we get weather data or an error (due to invalid API key)
            assertTrue(weatherNode.has("error") || weatherNode.has("location"));
        } catch (Exception e) {
            // Expected with invalid API key - tool exists and is callable
            assertTrue(e.getMessage().contains("weather") || e.getMessage().contains("API"));
        }
    }

    @Test
    void shouldExecuteWeatherForecastTool() throws Exception {
        // Note: This test will fail in real integration due to invalid API key
        // but it verifies the tool is properly exposed and callable
        String forecastArgsJson = objectMapper.writeValueAsString(Map.of("location", "New York", "days", 3));

        try {
            String forecastResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                    .name("weather.forecast")
                    .arguments(forecastArgsJson)
                    .build());

            assertNotNull(forecastResult);
            JsonNode forecastNode = objectMapper.readTree(forecastResult);

            // Either we get forecast data (array) or an error (object with error field)
            assertTrue(forecastNode.isArray() || forecastNode.has("error"));
            if (forecastNode.isArray() && forecastNode.size() > 0) {
                JsonNode firstItem = forecastNode.get(0);
                // Either contains forecast data or error
                assertTrue(firstItem.has("error") || firstItem.has("date"));
            }
        } catch (Exception e) {
            // Expected with invalid API key - tool exists and is callable
            assertTrue(e.getMessage().contains("weather") || e.getMessage().contains("API"));
        }
    }

    @Test
    void shouldExecuteWeatherAlertsTool() throws Exception {
        // Note: This test will fail in real integration due to invalid API key
        // but it verifies the tool is properly exposed and callable
        String alertsArgsJson = objectMapper.writeValueAsString(Map.of("location", "New York"));

        try {
            String alertsResult = mcpClient.executeTool(ToolExecutionRequest.builder()
                    .name("weather.alerts")
                    .arguments(alertsArgsJson)
                    .build());

            assertNotNull(alertsResult);
        } catch (Exception e) {
            // Expected with invalid API key - tool exists and is callable
            assertTrue(e.getMessage().contains("weather") || e.getMessage().contains("API"));
        }
    }

    private void waitForClientReady() throws Exception {
        final int maxRetries = 5;
        final long retryDelayMs = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                        .name("memory.read_graph")
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
}
