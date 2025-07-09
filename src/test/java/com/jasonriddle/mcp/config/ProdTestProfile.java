package com.jasonriddle.mcp.config;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/**
 * Test profile for production configuration validation.
 */
public final class ProdTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "prod";
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("weather.api.key", "test-api-key");
    }
}
