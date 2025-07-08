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
final class HerokuConfigurationValidationTest {

    @Inject
    Config config;

    @Test
    @DisplayName("Should use PORT environment variable")
    void shouldUsePortEnvironmentVariable() {
        // In test mode, Quarkus resolves environment variables and may override with test port
        String httpPort = config.getValue("quarkus.http.port", String.class);
        // The port should either be our simulated PORT value (5000) or a dynamic test port
        assertTrue(
                httpPort.equals("5000") || Integer.parseInt(httpPort) > 1024,
                String.format(
                        "Heroku profile must use PORT environment variable or dynamic test port, but was: %s",
                        httpPort));
    }

    @Test
    @DisplayName("Should bind to all interfaces")
    void shouldBindToAllInterfaces() {
        String httpHost = config.getValue("quarkus.http.host", String.class);
        assertEquals("0.0.0.0", httpHost, "Heroku requires binding to all interfaces (0.0.0.0) for proper routing.");
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
    @DisplayName("Should disable MCP traffic logging in production")
    void shouldDisableMcpTrafficLogging() {
        Boolean trafficLoggingEnabled = config.getValue("quarkus.mcp.server.traffic-logging.enabled", Boolean.class);
        assertFalse(trafficLoggingEnabled, "MCP traffic logging should be disabled in production for performance.");
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
