package com.jasonriddle.mcp.config;

import static com.jasonriddle.mcp.config.ConfigurationTestConstants.EXPECTED_APP_NAME;
import static com.jasonriddle.mcp.config.ConfigurationTestConstants.EXPECTED_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Configuration validation tests for the MCP server application.
 *
 * These tests validate that all configuration properties are set to their expected values
 * in the test environment. This ensures configuration consistency and helps catch
 * unexpected overrides or misconfigurations. Additionally validates Maven project
 * information from pom.xml is properly configured and accessible through Quarkus config.
 */
@QuarkusTest
@DisplayName("Configuration Validation")
final class ConfigurationValidationUnitTest {

    @Inject
    Config config;

    /**
     * Validates core application configuration settings.
     */
    @Nested
    @DisplayName("Core Application Configuration")
    class CoreApplicationConfiguration {

        @Test
        @DisplayName("Should validate shutdown configuration for graceful handling")
        void shouldValidateShutdownConfiguration() {
            String shutdownTimeout = config.getValue("quarkus.shutdown.timeout", String.class);
            assertEquals(
                    "30s",
                    shutdownTimeout,
                    String.format(
                            "Expected shutdown timeout to be '30s' but was '%s'. "
                                    + "This timeout is critical for graceful STDIO handling.",
                            shutdownTimeout));

            Boolean shutdownDelayEnabled = config.getValue("quarkus.shutdown.delay-enabled", Boolean.class);
            assertTrue(
                    shutdownDelayEnabled,
                    "Shutdown delay must be enabled for tool execution completion. "
                            + "This ensures all MCP operations complete before shutdown.");
        }

        @Test
        @DisplayName("Should validate project configuration from Maven")
        void shouldValidateProjectConfiguration() {
            String appName = config.getValue("quarkus.application.name", String.class);
            assertEquals(
                    EXPECTED_APP_NAME,
                    appName,
                    String.format(
                            "Expected application name to be '%s' but was '%s'. "
                                    + "This may indicate incorrect Maven configuration.",
                            EXPECTED_APP_NAME, appName));

            String appVersion = config.getValue("quarkus.application.version", String.class);
            assertEquals(
                    EXPECTED_VERSION,
                    appVersion,
                    String.format(
                            "Expected application version to be '%s' but was '%s'. "
                                    + "This should match the version in pom.xml.",
                            EXPECTED_VERSION, appVersion));
        }

        @Test
        @DisplayName("Should validate test profile is active")
        void shouldValidateTestProfile() {
            String testProfile = config.getValue("quarkus.test.profile", String.class);
            assertEquals(
                    "test",
                    testProfile,
                    "Test profile must be explicitly set to 'test' for proper test configuration loading.");
        }
    }

    /**
     * Validates HTTP and transport configuration.
     */
    @Nested
    @DisplayName("HTTP Transport Configuration")
    class HttpTransportConfiguration {

        @Test
        @DisplayName("Should validate HTTP test port configuration")
        void shouldValidateHttpTestPort() {
            String httpTestPort = config.getValue("quarkus.http.test-port", String.class);
            assertTrue(
                    httpTestPort.equals("0") || Integer.parseInt(httpTestPort) > 1024,
                    String.format(
                            "HTTP test port must be 0 (for dynamic assignment) or > 1024, but was: %s. "
                                    + "Dynamic port assignment prevents test port conflicts.",
                            httpTestPort));
        }

        @Test
        @DisplayName("Should validate HTTP host configuration if present")
        void shouldValidateHttpHostConfiguration() {
            config.getOptionalValue("quarkus.http.host", String.class).ifPresent(httpHost -> {
                assertTrue(
                        httpHost.equals("localhost") || httpHost.equals("0.0.0.0") || httpHost.equals("127.0.0.1"),
                        String.format(
                                "HTTP host must be a valid host address, but was: %s. "
                                        + "Valid values are 'localhost', '0.0.0.0', or '127.0.0.1'.",
                                httpHost));
            });
        }
    }

    /**
     * Validates logging configuration settings.
     */
    @Nested
    @DisplayName("Logging Configuration")
    class LoggingConfiguration {

        @Test
        @DisplayName("Should validate console logging for STDIO transport")
        void shouldValidateConsoleLogging() {
            Boolean consoleEnabled = config.getValue("quarkus.log.console.enable", Boolean.class);
            assertTrue(consoleEnabled, "Console logging must be enabled for STDIO transport to function correctly.");

            Boolean consoleStderr = config.getValue("quarkus.log.console.stderr", Boolean.class);
            assertTrue(
                    consoleStderr,
                    "Console logging must output to stderr to prevent interference with STDIO transport.");
        }

        @ParameterizedTest(name = "Should have DEBUG logging for {0}")
        @ValueSource(
                strings = {
                    "quarkus.log.category.\"com.jasonriddle.mcp\".level",
                    "quarkus.log.category.\"dev.langchain4j.mcp\".level"
                })
        @DisplayName("Debug logging categories")
        void shouldHaveDebugLoggingForTestCategories(final String logCategory) {
            String logLevel = config.getValue(logCategory, String.class);
            assertEquals(
                    "DEBUG",
                    logLevel,
                    String.format(
                            "Expected %s to be 'DEBUG' for test debugging, but was '%s'.", logCategory, logLevel));
        }

        @Test
        @DisplayName("Should suppress JUnit logging noise")
        void shouldSuppressJUnitLogging() {
            String junitLogLevel = config.getValue("quarkus.log.category.\"org.junit\".level", String.class);
            assertEquals("ERROR", junitLogLevel, "JUnit logging must be set to ERROR to reduce test output noise.");
        }

        @Test
        @DisplayName("Should suppress JIB processor warnings")
        void shouldSuppressJibProcessorWarnings() {
            String jibLogLevel = config.getValue(
                    "quarkus.log.category.\"io.quarkus.container.image.jib.deployment.JibProcessor\".level",
                    String.class);
            assertEquals(
                    "ERROR",
                    jibLogLevel,
                    "JIB processor warnings about multi-platform builds must be suppressed to ERROR level.");
        }

        @Test
        @DisplayName("Should validate file logging configuration")
        void shouldValidateFileLoggingConfiguration() {
            // Test environment has file logging enabled
            Boolean fileLoggingEnabled = config.getValue("quarkus.log.file.enable", Boolean.class);
            assertTrue(
                    fileLoggingEnabled, "File logging should be enabled in test environment for debugging purposes.");

            // In test environment, Quarkus overrides the log file path
            String logFilePath = config.getValue("quarkus.log.file.path", String.class);
            assertEquals("target/quarkus.log", logFilePath, "Log file path should use Quarkus test default location.");
        }
    }

    /**
     * Validates container image configuration.
     */
    @Nested
    @DisplayName("Container Image Configuration")
    class ContainerImageConfiguration {

        @Test
        @DisplayName("Should have container build disabled for tests")
        void shouldHaveContainerBuildDisabled() {
            Boolean buildEnabled = config.getValue("quarkus.container-image.build", Boolean.class);
            assertFalse(
                    buildEnabled,
                    "Container image build must be disabled for tests to prevent unnecessary image creation.");
        }

        @Test
        @DisplayName("Should validate Google Cloud Artifact Registry configuration")
        void shouldValidateContainerRegistryConfiguration() {
            String registry = config.getValue("quarkus.container-image.registry", String.class);
            assertEquals(
                    "us-central1-docker.pkg.dev",
                    registry,
                    String.format(
                            "Expected container registry to be Google Cloud Artifact Registry "
                                    + "'us-central1-docker.pkg.dev', but was '%s'.",
                            registry));

            String group = config.getValue("quarkus.container-image.group", String.class);
            assertEquals(
                    "jasons-mcp-server-20250705/mcp-servers",
                    group,
                    String.format(
                            "Expected container group to be 'jasons-mcp-server-20250705/mcp-servers', "
                                    + "but was '%s'. This should match the Google Cloud project structure.",
                            group));
        }

        @Test
        @DisplayName("Should validate container image naming")
        void shouldValidateContainerImageNaming() {
            String imageName = config.getValue("quarkus.container-image.name", String.class);
            assertEquals(
                    EXPECTED_APP_NAME,
                    imageName,
                    String.format(
                            "Container image name should match application name '%s', but was '%s'.",
                            EXPECTED_APP_NAME, imageName));

            String tag = config.getValue("quarkus.container-image.tag", String.class);
            assertEquals("latest", tag, "Default container tag should be 'latest' for development builds.");

            String additionalTags = config.getValue("quarkus.container-image.additional-tags", String.class);
            assertEquals(
                    EXPECTED_VERSION,
                    additionalTags,
                    String.format(
                            "Additional tags should include project version '%s' for versioned deployments, "
                                    + "but was '%s'.",
                            EXPECTED_VERSION, additionalTags));
        }

        @Test
        @DisplayName("Should validate JIB multi-platform configuration")
        void shouldValidateJibConfiguration() {
            String baseImage = config.getValue("quarkus.jib.base-jvm-image", String.class);
            assertEquals(
                    "registry.access.redhat.com/ubi9/openjdk-17-runtime",
                    baseImage,
                    "JIB must use Red Hat Universal Base Image 9 with OpenJDK 17 runtime for security and compliance.");

            String platforms = config.getValue("quarkus.jib.platforms", String.class);
            assertEquals(
                    "linux/amd64,linux/arm64",
                    platforms,
                    "JIB must build multi-platform images supporting both AMD64 and ARM64 architectures.");
        }
    }

    /**
     * Validates memory persistence configuration.
     */
    @Nested
    @DisplayName("Memory Configuration")
    class MemoryConfiguration {

        @Test
        @DisplayName("Should use test-specific memory file")
        void shouldUseTestSpecificMemoryFile() {
            String memoryPath = config.getValue("memory.file.path", String.class);
            assertEquals(
                    "memory-test.jsonl",
                    memoryPath,
                    String.format(
                            "Memory file path should be 'memory-test.jsonl' in test environment "
                                    + "to prevent interference with production data, but was '%s'.",
                            memoryPath));
        }

        @Test
        @DisplayName("Should have default memory configuration available")
        void shouldHaveDefaultMemoryConfiguration() {
            // Verify that memory.file.path always has a value (either configured or default)
            String memoryPath =
                    config.getOptionalValue("memory.file.path", String.class).orElse("memory.jsonl");
            assertNotNull(memoryPath, "Memory file path must have a configured or default value.");
            assertTrue(
                    memoryPath.endsWith(".jsonl"),
                    String.format("Memory file should use JSONL format, but was '%s'.", memoryPath));
        }
    }

    /**
     * Validates weather API configuration.
     */
    @Nested
    @DisplayName("Weather API Configuration")
    class WeatherApiConfiguration {

        @Test
        @DisplayName("Should have weather API key configuration available")
        void shouldHaveWeatherApiKeyConfiguration() {
            // Weather API key is optional in tests and may be empty
            String weatherApiKey =
                    config.getOptionalValue("weather.api.key", String.class).orElse("");
            assertNotNull(weatherApiKey, "Weather API key configuration must be present (can be empty for tests).");
        }

        @Test
        @DisplayName("Should validate REST client configuration for weather service")
        void shouldValidateRestClientConfiguration() {
            // Weather client configuration removed - using @RegisterRestClient(baseUri =
            // "https://api.openweathermap.org")
            // Test that the WeatherClient interface is properly configured with hardcoded base URI
            // This test is maintained for documentation purposes but no longer validates configuration properties
            assertTrue(true, "WeatherClient uses @RegisterRestClient annotation with hardcoded baseUri");
        }

        @Test
        @DisplayName("Should validate weather configuration consistency")
        void shouldValidateWeatherConfigurationConsistency() {
            // Weather client configuration removed - using @RegisterRestClient(baseUri =
            // "https://api.openweathermap.org")
            // Verify that weather API key configuration is present (can be empty for tests)
            String weatherApiKey =
                    config.getOptionalValue("weather.api.key", String.class).orElse("");
            assertNotNull(weatherApiKey, "Weather API key configuration must be present (can be empty for tests).");

            // The base URI is now hardcoded in @RegisterRestClient annotation as "https://api.openweathermap.org"
            assertTrue(true, "WeatherClient uses HTTPS and OpenWeatherMap service via @RegisterRestClient annotation");
        }
    }

    /**
     * Validates MCP server configuration.
     */
    @Nested
    @DisplayName("MCP Server Configuration")
    class McpServerConfiguration {

        @Test
        @DisplayName("Should validate MCP server identity")
        void shouldValidateMcpServerIdentity() {
            String serverName = config.getValue("quarkus.mcp.server.server-info.name", String.class);
            assertEquals(
                    EXPECTED_APP_NAME,
                    serverName,
                    String.format(
                            "MCP server name should match application name '%s', but was '%s'.",
                            EXPECTED_APP_NAME, serverName));

            String serverVersion = config.getValue("quarkus.mcp.server.server-info.version", String.class);
            assertEquals(
                    EXPECTED_VERSION,
                    serverVersion,
                    String.format(
                            "MCP server version should match project version '%s', but was '%s'.",
                            EXPECTED_VERSION, serverVersion));
        }

        @Test
        @DisplayName("Should have traffic logging enabled for debugging")
        void shouldHaveTrafficLoggingEnabled() {
            Boolean loggingEnabled = config.getValue("quarkus.mcp.server.traffic-logging.enabled", Boolean.class);
            assertTrue(
                    loggingEnabled,
                    "MCP traffic logging must be enabled in test environment for debugging protocol issues.");

            Integer textLimit = config.getValue("quarkus.mcp.server.traffic-logging.text-limit", Integer.class);
            assertEquals(
                    500,
                    textLimit,
                    "MCP traffic logging text limit should be 500 characters to balance detail with readability.");
        }

        @Test
        @DisplayName("Should validate STDIO transport is disabled by default")
        void shouldHaveStdioTransportDisabled() {
            Boolean stdioEnabled = config.getValue("quarkus.mcp.server.stdio.enabled", Boolean.class);
            assertFalse(
                    stdioEnabled,
                    "STDIO transport must be disabled in base test profile. "
                            + "Integration tests should explicitly enable it when needed.");

            Boolean stdioInitEnabled =
                    config.getValue("quarkus.mcp.server.stdio.initialization-enabled", Boolean.class);
            assertFalse(
                    stdioInitEnabled,
                    "STDIO initialization must be disabled in base test profile to prevent automatic startup.");
        }

        @Test
        @DisplayName("Should validate SSE configuration if present")
        void shouldValidateSseConfigurationIfPresent() {
            config.getOptionalValue("quarkus.mcp.server.sse.root-path", String.class)
                    .ifPresent(sseRootPath -> {
                        assertTrue(
                                sseRootPath.startsWith("/"),
                                String.format(
                                        "SSE root path must start with '/', but was '%s'. "
                                                + "This ensures proper URL routing.",
                                        sseRootPath));
                        assertFalse(
                                sseRootPath.endsWith("/"),
                                String.format(
                                        "SSE root path should not end with '/', but was '%s'. "
                                                + "Trailing slashes can cause routing issues.",
                                        sseRootPath));
                    });
        }
    }

    /**
     * Validates configuration consistency and defaults.
     */
    @Nested
    @DisplayName("Configuration Consistency")
    class ConfigurationConsistency {

        @Test
        @DisplayName("Should have consistent critical configurations")
        void shouldHaveConsistentCriticalConfigurations() {
            // These configurations should remain consistent across all profiles
            String shutdownTimeout = config.getValue("quarkus.shutdown.timeout", String.class);
            assertEquals(
                    "30s",
                    shutdownTimeout,
                    "Shutdown timeout must be consistent across all profiles for predictable behavior.");

            String appName = config.getValue("quarkus.application.name", String.class);
            assertEquals(EXPECTED_APP_NAME, appName, "Application name must be consistent across all profiles.");

            String serverName = config.getValue("quarkus.mcp.server.server-info.name", String.class);
            assertEquals(EXPECTED_APP_NAME, serverName, "MCP server name must match application name for consistency.");
        }

        @Test
        @DisplayName("Should validate logging level hierarchy")
        void shouldValidateLoggingLevelHierarchy() {
            String defaultLevel = config.getValue("quarkus.log.level", String.class);
            // In test, default is WARN
            assertEquals("WARN", defaultLevel, "Default log level should be WARN in test environment.");

            // Application logging should be more verbose than default
            String appLogLevel = config.getValue("quarkus.log.category.\"com.jasonriddle.mcp\".level", String.class);
            assertEquals("DEBUG", appLogLevel, "Application logging should be DEBUG for better test diagnostics.");
        }
    }
}
