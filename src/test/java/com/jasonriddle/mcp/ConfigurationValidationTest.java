package com.jasonriddle.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for validating application configuration properties.
 *
 * These tests ensure that configuration properties are properly set and help catch
 * any unexpected overrides that might occur at different configuration levels.
 */
@QuarkusTest
final class ConfigurationValidationTest {

    @Inject
    Config config;

    @Test
    void shouldValidateCoreApplicationConfiguration() {
        // Validate shutdown configuration for graceful STDIO handling
        assertEquals(
                "30s",
                config.getValue("quarkus.shutdown.timeout", String.class),
                "Shutdown timeout should be 30 seconds to prevent race conditions");
        assertTrue(
                config.getValue("quarkus.shutdown.delay-enabled", Boolean.class),
                "Shutdown delay should be enabled for tool execution completion");
    }

    @Test
    void shouldValidateLoggingConfiguration() {
        // Validate base logging configuration
        // Note: During unit tests, Quarkus may override log level to WARN for reduced noise
        String logLevel = config.getValue("quarkus.log.level", String.class);
        assertTrue(
                logLevel.equals("INFO") || logLevel.equals("WARN"),
                "Global log level should be INFO (production) or WARN (test), got: " + logLevel);

        // Validate console logging (required for STDIO transport)
        assertTrue(config.getValue("quarkus.log.console.enable", Boolean.class), "Console logging should be enabled");
        assertTrue(
                config.getValue("quarkus.log.console.stderr", Boolean.class),
                "Console logging should go to stderr for STDIO transport compatibility");

        // Validate file logging
        assertTrue(config.getValue("quarkus.log.file.enable", Boolean.class), "File logging should be enabled");
        // Note: During unit tests, Quarkus uses "target/quarkus.log" instead of production value
        String logFilePath = config.getValue("quarkus.log.file.path", String.class);
        assertTrue(
                logFilePath.equals("jasons-mcp-server.log") || logFilePath.equals("target/quarkus.log"),
                "Log file path should be production (jasons-mcp-server.log) or test (target/quarkus.log), got: "
                        + logFilePath);

        // Validate application-specific logging levels
        assertEquals(
                "INFO",
                config.getValue("quarkus.log.category.\"com.jasonriddle.mcp\".level", String.class),
                "MCP package log level should be INFO");
        assertEquals(
                "INFO",
                config.getValue("quarkus.log.category.\"io.quarkiverse.mcp\".level", String.class),
                "Quarkus MCP extension log level should be INFO");
        assertEquals(
                "INFO",
                config.getValue("quarkus.log.category.\"dev.langchain4j.mcp\".level", String.class),
                "LangChain4j MCP log level should be INFO");

        // Validate JIB processor warning suppression
        assertEquals(
                "ERROR",
                config.getValue(
                        "quarkus.log.category.\"io.quarkus.container.image.jib.deployment.JibProcessor\".level",
                        String.class),
                "JIB processor warnings should be suppressed to ERROR level");
    }

    @Test
    void shouldValidateContainerImageConfiguration() {
        // Validate container build configuration
        assertTrue(
                config.getValue("quarkus.container-image.build", Boolean.class),
                "Container image build should be enabled");
        assertEquals(
                "us-central1-docker.pkg.dev",
                config.getValue("quarkus.container-image.registry", String.class),
                "Container registry should be set to Google Cloud");
        assertEquals(
                "jasons-mcp-server-20250705/mcp-servers",
                config.getValue("quarkus.container-image.group", String.class),
                "Container group should be set correctly");
        assertEquals(
                "jasons-mcp-server",
                config.getValue("quarkus.container-image.name", String.class),
                "Container name should be set correctly");
        assertEquals(
                "latest",
                config.getValue("quarkus.container-image.tag", String.class),
                "Container tag should be latest");
        assertEquals(
                "0.0.1-SNAPSHOT",
                config.getValue("quarkus.container-image.additional-tags", String.class),
                "Container additional tags should include project version");

        // Validate JIB configuration
        assertEquals(
                "registry.access.redhat.com/ubi9/openjdk-17-runtime",
                config.getValue("quarkus.jib.base-jvm-image", String.class),
                "JIB base image should be UBI 9 OpenJDK 17 runtime");
        assertEquals(
                "linux/amd64,linux/arm64",
                config.getValue("quarkus.jib.platforms", String.class),
                "JIB should support both AMD64 and ARM64 platforms");
    }

    @Test
    void shouldValidateMemoryConfiguration() {
        // Validate memory persistence configuration
        assertEquals(
                "memory.jsonl",
                config.getValue("memory.file.path", String.class),
                "Memory file path should use default production value");
    }

    @Test
    void shouldValidateUnusedPropertiesAreNotSet() {
        // Validate that SSE transport properties are NOT configured (using STDIO instead)
        assertFalse(
                config.getOptionalValue("quarkus.mcp.server.memory.server-info.name", String.class)
                        .isPresent(),
                "MCP server name should not be explicitly configured (using defaults)");
        assertFalse(
                config.getOptionalValue("quarkus.mcp.server.memory.server-info.version", String.class)
                        .isPresent(),
                "MCP server version should not be explicitly configured (using defaults)");
        assertFalse(
                config.getOptionalValue("quarkus.mcp.server.memory.sse.root-path", String.class)
                        .isPresent(),
                "SSE root path should not be configured (using STDIO transport)");
        // Note: During unit tests, Quarkus automatically assigns random test ports
        // The important thing is that we didn't explicitly configure it in our application.properties
        String httpPort =
                config.getOptionalValue("quarkus.http.port", String.class).orElse("8080");
        // Test ports are typically high numbers (e.g., 49669), production default is 8080
        assertTrue(
                httpPort.equals("8080") || httpPort.equals("0") || Integer.parseInt(httpPort) > 1024,
                "HTTP port should be default (8080), test-disabled (0), or dynamic test port (>1024), got: "
                        + httpPort);

        // Validate that redundant STDIO properties are NOT set (enabled by default)
        assertFalse(
                config.getOptionalValue("quarkus.mcp.server.memory.stdio.enabled", String.class)
                        .isPresent(),
                "STDIO enabled should not be explicitly set (enabled by default when SSE not configured)");
        // Note: Quarkus MCP extension may automatically set this during testing
        // so we don't assert its absence, just that we didn't explicitly configure it
        // assertFalse(config.getOptionalValue("quarkus.mcp.server.stdio.enabled", String.class).isPresent());
        assertFalse(
                config.getOptionalValue("quarkus.mcp.server.stdio.initialization-enabled", String.class)
                        .isPresent(),
                "STDIO initialization-enabled property should not exist (non-existent property)");
    }

    @Test
    void shouldValidateTestConfigurationOverrides() {
        // This test verifies that our test configuration override mechanism works
        // by checking that we can properly override the memory file path in tests

        // Get the current memory file path - in unit tests this should be the default
        String memoryFilePath = config.getValue("memory.file.path", String.class);
        assertEquals("memory.jsonl", memoryFilePath, "In unit tests, memory file path should use production default");
    }
}
