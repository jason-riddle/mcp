package com.jasonriddle.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.mcp.client.McpClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for MCP integration tests providing minimal shared infrastructure.
 *
 * This class only handles client setup/teardown and file cleanup.
 * All test logic is implemented in the specific test classes.
 */
abstract class McpIntegrationTestBase {

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
     * Cleans up the test memory file to ensure test isolation.
     */
    private void cleanupTestMemoryFile() {
        try {
            Path memoryFile = Paths.get(getTestMemoryFile());
            Files.deleteIfExists(memoryFile);
        } catch (IOException e) {
            // Ignore cleanup failures
        }
    }
}
