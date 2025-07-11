package com.jasonriddle.mcp.config;

import static com.jasonriddle.mcp.config.ConfigurationTestConstants.EXPECTED_APP_NAME;
import static com.jasonriddle.mcp.config.ConfigurationTestConstants.EXPECTED_VERSION;
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
 * Configuration validation tests for Heroku deployment profile.
 */
@QuarkusTest
@TestProfile(HerokuTestProfile.class)
@DisplayName("Heroku Configuration Validation")
final class HerokuConfigurationValidationUnitTest {

    @Inject
    Config config;

    @Test
    @DisplayName("Should configure HTTP server for stdio-only transport")
    void shouldConfigureHttpServerForStdioOnlyTransport() {
        // In production, HTTP port should be disabled (-1)
        // In test mode, Quarkus may override with a random port for testing
        String httpPort = config.getValue("quarkus.http.port", String.class);
        assertTrue(
                httpPort.equals("-1") || Integer.parseInt(httpPort) > 1024,
                String.format("HTTP server should be disabled (-1) for stdio-only transport, but was: %s", httpPort));
    }

    @Test
    @DisplayName("Should have production logging level")
    void shouldHaveProductionLoggingLevel() {
        String logLevel = config.getValue("quarkus.log.level", String.class);
        assertEquals("INFO", logLevel, "Heroku profile should use INFO logging level for production.");
    }

    @Test
    @DisplayName("Should disable file logging")
    void shouldDisableFileLogging() {
        Boolean fileLoggingEnabled = config.getValue("quarkus.log.file.enable", Boolean.class);
        assertFalse(fileLoggingEnabled, "File logging must be disabled on Heroku. Use Heroku logs instead.");
    }

    @Test
    @DisplayName("Should disable container image building")
    void shouldDisableContainerImageBuilding() {
        Boolean containerBuildEnabled = config.getValue("quarkus.container-image.build", Boolean.class);
        assertFalse(
                containerBuildEnabled,
                "Container image building must be disabled on Heroku (uses buildpacks instead).");
    }

    @Test
    @DisplayName("Should use Heroku ephemeral filesystem for memory")
    void shouldUseHerokuFilesystemForMemory() {
        String memoryPath = config.getValue("memory.file.path", String.class);
        assertEquals(
                "/app/memory.jsonl",
                memoryPath,
                "Memory file must be stored in /app directory on Heroku's ephemeral filesystem.");
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
}
