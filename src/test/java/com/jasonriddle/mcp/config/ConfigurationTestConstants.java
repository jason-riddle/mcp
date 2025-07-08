package com.jasonriddle.mcp.config;

/**
 * Shared constants for configuration validation tests.
 */
public final class ConfigurationTestConstants {

    /**
     * Expected application version from Maven project configuration.
     */
    public static final String EXPECTED_VERSION = "0.0.1-SNAPSHOT";

    /**
     * Expected application name from Maven project configuration.
     */
    public static final String EXPECTED_APP_NAME = "jasons-mcp-server";

    private ConfigurationTestConstants() {
        // Utility class - prevent instantiation
    }
}
