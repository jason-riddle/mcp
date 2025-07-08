package com.jasonriddle.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Configuration validation tests for production deployment profile.
 */
@QuarkusTest
@TestProfile(ProdTestProfile.class)
@DisplayName("Production Configuration Validation")
final class ProdConfigurationValidationTest {

    private static final String EXPECTED_VERSION = "0.0.1-SNAPSHOT";
    private static final String EXPECTED_APP_NAME = "jasons-mcp-server";

    @Inject
    Config config;

    @Test
    @DisplayName("Should use production logging level")
    void shouldUseProductionLoggingLevel() {
        String logLevel = config.getValue("quarkus.log.level", String.class);
        assertEquals("INFO", logLevel, "Production profile should use INFO logging level.");
    }

    @Test
    @DisplayName("Should enable file logging for production")
    void shouldEnableFileLogging() {
        Boolean fileLoggingEnabled = config.getValue("quarkus.log.file.enable", Boolean.class);
        assertTrue(fileLoggingEnabled, "File logging should be enabled in production for audit and debugging.");

        // In test mode, Quarkus overrides log file path
        String logFilePath = config.getValue("quarkus.log.file.path", String.class);
        assertEquals("target/quarkus.log", logFilePath, "In test mode, log file path uses Quarkus test default.");
    }

    @Test
    @DisplayName("Should disable container image building")
    void shouldDisableContainerImageBuilding() {
        Boolean containerBuildEnabled = config.getValue("quarkus.container-image.build", Boolean.class);
        assertFalse(
                containerBuildEnabled,
                "Container image building should be disabled in production (pre-built images used).");
    }

    @Test
    @DisplayName("Should use test memory configuration in test mode")
    void shouldUseTestMemoryConfiguration() {
        // In test mode, test configuration takes precedence
        String memoryPath = config.getValue("memory.file.path", String.class);
        assertEquals("memory-test.jsonl", memoryPath, "Test mode overrides memory file path.");
    }

    @Test
    @DisplayName("Should disable MCP traffic logging")
    void shouldDisableMcpTrafficLogging() {
        Boolean trafficLoggingEnabled = config.getValue("quarkus.mcp.server.traffic-logging.enabled", Boolean.class);
        assertFalse(
                trafficLoggingEnabled,
                "MCP traffic logging must be disabled in production for performance and security.");

        Integer textLimit = config.getValue("quarkus.mcp.server.traffic-logging.text-limit", Integer.class);
        assertEquals(500, textLimit, "Traffic logging text limit should remain at default even when disabled.");
    }

    @Test
    @DisplayName("Should maintain consistent core configurations")
    void shouldMaintainConsistentCoreConfigurations() {
        // These should remain the same across all profiles
        assertEquals(
                EXPECTED_APP_NAME,
                config.getValue("quarkus.application.name", String.class),
                "Application name must remain consistent.");
        assertEquals(
                EXPECTED_VERSION,
                config.getValue("quarkus.application.version", String.class),
                "Application version must remain consistent.");
        assertEquals(
                "30s",
                config.getValue("quarkus.shutdown.timeout", String.class),
                "Shutdown timeout must remain consistent.");
        assertTrue(
                config.getValue("quarkus.shutdown.delay-enabled", Boolean.class),
                "Shutdown delay must remain enabled.");
    }

    @Test
    @DisplayName("Should have appropriate HTTP port configuration")
    void shouldHaveAppropriateHttpPortConfiguration() {
        // In test mode, Quarkus assigns a dynamic port
        String httpPort = config.getValue("quarkus.http.port", String.class);
        assertTrue(
                Integer.parseInt(httpPort) > 1024,
                String.format("Test mode should use dynamic port > 1024, but was: %s", httpPort));
    }

    @Test
    @DisplayName("Should have test logging configuration in test mode")
    void shouldHaveTestLoggingConfiguration() {
        // In test mode, test configuration takes precedence over production configuration
        String appLogLevel = config.getValue("quarkus.log.category.\"com.jasonriddle.mcp\".level", String.class);
        assertEquals("DEBUG", appLogLevel, "Test mode sets application logging to DEBUG.");

        String mcpLogLevel = config.getValue("quarkus.log.category.\"dev.langchain4j.mcp\".level", String.class);
        assertEquals("DEBUG", mcpLogLevel, "Test mode sets MCP library logging to DEBUG.");
    }
}
