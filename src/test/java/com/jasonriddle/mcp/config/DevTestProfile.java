package com.jasonriddle.mcp.config;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/**
 * Test profile for development configuration validation.
 */
public final class DevTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "dev";
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("weather.api.key", "test-api-key");
    }
}
