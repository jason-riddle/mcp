package com.jasonriddle.mcp.config;

import static com.jasonriddle.mcp.config.ConfigurationTestConstants.EXPECTED_APP_NAME;
import static com.jasonriddle.mcp.config.ConfigurationTestConstants.EXPECTED_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Configuration validation tests for development deployment profile.
 */
@QuarkusTest
@TestProfile(DevTestProfile.class)
@DisplayName("Development Configuration Validation")
final class DevConfigurationValidationUnitTest {

    @Inject
    Config config;

    @Test
    @DisplayName("Should use development logging level")
    void shouldUseDevelopmentLoggingLevel() {
        String logLevel = config.getValue("quarkus.log.level", String.class);
        assertEquals("DEBUG", logLevel, "Development profile should use DEBUG logging level.");
    }

    @Test
    @DisplayName("Should enable file logging for development")
    void shouldEnableFileLogging() {
        Boolean fileLoggingEnabled = config.getValue("quarkus.log.file.enable", Boolean.class);
        assertTrue(fileLoggingEnabled, "File logging should be enabled in development for debugging.");

        // In test mode, Quarkus overrides log file path
        String logFilePath = config.getValue("quarkus.log.file.path", String.class);
        assertEquals("target/quarkus.log", logFilePath, "In test mode, log file path uses Quarkus test default.");
    }

    @Test
    @DisplayName("Should enable container image building")
    void shouldEnableContainerImageBuilding() {
        Boolean containerBuildEnabled = config.getValue("quarkus.container-image.build", Boolean.class);
        assertTrue(
                containerBuildEnabled, "Container image building should be enabled in development for local testing.");
    }

    @Test
    @DisplayName("Should use test memory configuration in test mode")
    void shouldUseTestMemoryConfiguration() {
        // In test mode, test configuration takes precedence
        String memoryPath = config.getValue("memory.file.path", String.class);
        assertEquals("memory-test.jsonl", memoryPath, "Test mode overrides memory file path.");
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
    @DisplayName("Should have development logging configuration enabled")
    void shouldHaveDevelopmentLoggingConfiguration() {
        // In development mode, debug logging is enabled
        String appLogLevel = config.getValue("quarkus.log.category.\"com.jasonriddle.mcp\".level", String.class);
        assertEquals("DEBUG", appLogLevel, "Development mode sets application logging to DEBUG.");

        String mcpLogLevel = config.getValue("quarkus.log.category.\"dev.langchain4j.mcp\".level", String.class);
        assertEquals("DEBUG", mcpLogLevel, "Development mode sets MCP library logging to DEBUG.");
    }
}
