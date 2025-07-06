package com.jasonriddle.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for validating runtime application configuration properties.
 *
 * These tests focus on configuration values that are available and testable at runtime,
 * ensuring that critical runtime properties are properly set and help catch
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
                "DEBUG",
                config.getValue("quarkus.log.category.\"com.jasonriddle.mcp\".level", String.class),
                "MCP package log level should be DEBUG");
        assertEquals(
                "DEBUG",
                config.getValue("quarkus.log.category.\"io.quarkiverse.mcp\".level", String.class),
                "Quarkus MCP extension log level should be DEBUG");
        assertEquals(
                "DEBUG",
                config.getValue("quarkus.log.category.\"dev.langchain4j.mcp\".level", String.class),
                "LangChain4j MCP log level should be DEBUG");
    }

    @Test
    void shouldValidateMemoryConfiguration() {
        // Validate memory persistence configuration
        assertEquals(
                "memory.jsonl",
                config.getValue("memory.file.path", String.class),
                "Memory file path should use default production value");
    }
}
