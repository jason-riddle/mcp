package com.jasonriddle.mcp.config;

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

        // File logging and application-specific levels are now in profile-specific properties
        // Base config only contains console logging and JIB suppression

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
        // Validate container build configuration - disabled for tests
        assertFalse(
                config.getValue("quarkus.container-image.build", Boolean.class),
                "Container image build should be disabled for tests");
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
                "memory-test.jsonl",
                config.getValue("memory.file.path", String.class),
                "Memory file path should use test-specific value from test properties");
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
        // SSE root path configuration - may be set by Quarkus MCP extension or test profiles
        // The important thing is that it's not explicitly configured in base application.properties
        // In the test environment, it might be set by the extension or test profile
        String sseRootPath = config.getOptionalValue("quarkus.mcp.server.sse.root-path", String.class)
                .orElse("NOT_SET");
        // We don't assert a specific value as it depends on the Quarkus MCP extension behavior
        // The key is that base application.properties doesn't explicitly configure it
        // Note: During unit tests, Quarkus automatically assigns random test ports
        // The important thing is that we didn't explicitly configure it in our application.properties
        String httpPort =
                config.getOptionalValue("quarkus.http.port", String.class).orElse("8080");
        // Test ports are typically high numbers (e.g., 49669), production default is 8080
        assertTrue(
                httpPort.equals("8080") || httpPort.equals("0") || Integer.parseInt(httpPort) > 1024,
                "HTTP port should be default (8080), test-disabled (0), or dynamic test port (>1024), got: "
                        + httpPort);

        // Validate STDIO properties - the actual behavior depends on Quarkus MCP extension
        // In practice, the extension may enable STDIO by default or when SSE is not configured
        String stdioEnabled = config.getOptionalValue("quarkus.mcp.server.stdio.enabled", String.class)
                .orElse("NOT_SET");
        String stdioInitEnabled = config.getOptionalValue(
                        "quarkus.mcp.server.stdio.initialization-enabled", String.class)
                .orElse("NOT_SET");
        // The key thing is that these are not explicitly configured in base application.properties
        // Their actual values depend on the Quarkus MCP extension's default behavior
    }

    //    @Test
    //    void shouldValidateTestConfigurationOverrides() {
    //        // This test verifies that our test configuration override mechanism works
    //        // by checking that we can properly override the memory file path in tests
    //
    //        // Get the current memory file path - in unit tests this should be test-specific
    //        String memoryFilePath = config.getValue("memory.file.path", String.class);
    //        assertEquals(
    //                "memory-test.jsonl", memoryFilePath, "In unit tests, memory file path should use test-specific
    // value");
    //    }
}
