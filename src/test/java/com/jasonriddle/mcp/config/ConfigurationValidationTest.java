package com.jasonriddle.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

/**
 * Configuration validation tests for the MCP server application.
 *
 * These tests validate that all configuration properties are set to their expected values
 * in the test environment. This ensures configuration consistency and helps catch
 * unexpected overrides or misconfigurations. Additionally validates Maven project
 * information from pom.xml is properly configured and accessible through Quarkus config.
 */
@QuarkusTest
final class ConfigurationValidationTest {

    private static final String EXPECTED_VERSION = "0.0.1-SNAPSHOT";

    @Inject
    Config config;

    @Test
    void shouldValidateShutdownConfiguration() {
        assertEquals(
                "30s",
                config.getValue("quarkus.shutdown.timeout", String.class),
                "Shutdown timeout must be 30 seconds for graceful STDIO handling");
        assertTrue(
                config.getValue("quarkus.shutdown.delay-enabled", Boolean.class),
                "Shutdown delay must be enabled for tool execution completion");
    }

    @Test
    void shouldValidateHttpConfiguration() {
        // Validate that test profile is set
        assertEquals(
                "test",
                config.getValue("quarkus.test.profile", String.class),
                "Test profile must be explicitly set to test");

        // Validate that HTTP test port is configured for dynamic assignment
        // Note: During runtime, Quarkus may override this with the actual assigned port
        String httpTestPort = config.getValue("quarkus.http.test-port", String.class);
        assertTrue(
                httpTestPort.equals("0") || Integer.parseInt(httpTestPort) > 1024,
                "HTTP test port must be 0 (configured) or a dynamic port (>1024), got: " + httpTestPort);
    }

    @Test
    void shouldValidateProjectConfiguration() {
        // Validate that Maven project information is properly configured
        assertEquals(
                "jasons-mcp-server",
                config.getValue("quarkus.application.name", String.class),
                "Application name must be set to jasons-mcp-server");
        assertEquals(
                EXPECTED_VERSION,
                config.getValue("quarkus.application.version", String.class),
                "Maven version must be set to " + EXPECTED_VERSION);
    }

    @Test
    void shouldValidateLoggingConfiguration() {
        // Console logging configuration
        assertTrue(
                config.getValue("quarkus.log.console.enable", Boolean.class),
                "Console logging must be enabled for STDIO transport");
        assertTrue(
                config.getValue("quarkus.log.console.stderr", Boolean.class),
                "Console logging must output to stderr for STDIO transport compatibility");

        // Test-specific logging levels
        assertEquals(
                "ERROR",
                config.getValue("quarkus.log.category.\"org.junit\".level", String.class),
                "JUnit logging must be set to ERROR to reduce test noise");
        assertEquals(
                "DEBUG",
                config.getValue("quarkus.log.category.\"com.jasonriddle.mcp\".level", String.class),
                "Application logging must be set to DEBUG for test debugging");
        assertEquals(
                "DEBUG",
                config.getValue("quarkus.log.category.\"dev.langchain4j.mcp\".level", String.class),
                "MCP library logging must be set to DEBUG for test debugging");

        // JIB processor warning suppression
        assertEquals(
                "ERROR",
                config.getValue(
                        "quarkus.log.category.\"io.quarkus.container.image.jib.deployment.JibProcessor\".level",
                        String.class),
                "JIB processor warnings must be suppressed to ERROR level");
    }

    @Test
    void shouldValidateContainerImageConfiguration() {
        // Container build must be disabled for tests
        assertFalse(
                config.getValue("quarkus.container-image.build", Boolean.class),
                "Container image build must be disabled for tests");

        // Container registry configuration
        assertEquals(
                "us-central1-docker.pkg.dev",
                config.getValue("quarkus.container-image.registry", String.class),
                "Container registry must be set to Google Cloud Artifact Registry");
        assertEquals(
                "jasons-mcp-server-20250705/mcp-servers",
                config.getValue("quarkus.container-image.group", String.class),
                "Container group must be set to project/repository path");
        assertEquals(
                "jasons-mcp-server",
                config.getValue("quarkus.container-image.name", String.class),
                "Container name must be set to jasons-mcp-server");
        assertEquals(
                "latest",
                config.getValue("quarkus.container-image.tag", String.class),
                "Container tag must be set to latest");
        assertEquals(
                EXPECTED_VERSION,
                config.getValue("quarkus.container-image.additional-tags", String.class),
                "Container additional tags must include project version");

        // JIB configuration
        assertEquals(
                "registry.access.redhat.com/ubi9/openjdk-17-runtime",
                config.getValue("quarkus.jib.base-jvm-image", String.class),
                "JIB base image must be UBI 9 OpenJDK 17 runtime");
        assertEquals(
                "linux/amd64,linux/arm64",
                config.getValue("quarkus.jib.platforms", String.class),
                "JIB must support both AMD64 and ARM64 platforms");
    }

    @Test
    void shouldValidateMemoryConfiguration() {
        assertEquals(
                "memory-test.jsonl",
                config.getValue("memory.file.path", String.class),
                "Memory file path must use test-specific value in test environment");
    }

    @Test
    void shouldValidateMcpServerConfiguration() {
        // MCP server information
        assertEquals(
                "jasons-mcp-server",
                config.getValue("quarkus.mcp.server.server-info.name", String.class),
                "MCP server name must be set to jasons-mcp-server");
        assertEquals(
                EXPECTED_VERSION,
                config.getValue("quarkus.mcp.server.server-info.version", String.class),
                "MCP server version must be set to project version");

        // MCP traffic logging (enabled for tests)
        assertTrue(
                config.getValue("quarkus.mcp.server.traffic-logging.enabled", Boolean.class),
                "MCP traffic logging must be enabled for test debugging");
        assertEquals(
                500,
                config.getValue("quarkus.mcp.server.traffic-logging.text-limit", Integer.class),
                "MCP traffic logging text limit must be set to 500");
    }

    @Test
    void shouldValidateStdioTransportConfiguration() {
        // STDIO transport must be disabled in base test profile
        assertFalse(
                config.getValue("quarkus.mcp.server.stdio.enabled", Boolean.class),
                "STDIO transport must be disabled in base test profile");
        assertFalse(
                config.getValue("quarkus.mcp.server.stdio.initialization-enabled", Boolean.class),
                "STDIO initialization must be disabled in base test profile");
    }

    @Test
    void shouldValidateAutomaticQuarkusTestConfiguration() {
        // Quarkus may automatically configure certain properties during testing
        // We validate that if HTTP host is configured, it has a reasonable value
        String httpHost =
                config.getOptionalValue("quarkus.http.host", String.class).orElse(null);
        if (httpHost != null) {
            assertTrue(
                    httpHost.equals("localhost") || httpHost.equals("0.0.0.0") || httpHost.equals("127.0.0.1"),
                    "HTTP host must be a valid host if configured, got: " + httpHost);
        }
    }

    @Test
    void shouldValidateAutomaticMcpExtensionConfiguration() {
        // SSE transport properties may be automatically configured by MCP extension
        // We validate that if SSE root path is configured, it has a reasonable value
        String sseRootPath = config.getOptionalValue("quarkus.mcp.server.sse.root-path", String.class)
                .orElse(null);
        if (sseRootPath != null) {
            assertTrue(
                    sseRootPath.startsWith("/"),
                    "SSE root path must start with '/' if configured, got: " + sseRootPath);
        }
    }
}
