package com.jasonriddle.mcp.config;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.HashMap;
import java.util.Map;

/**
 * Test profile for Heroku configuration validation.
 */
public final class HerokuTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "heroku";
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> overrides = new HashMap<>();
        // Simulate Heroku environment
        overrides.put("PORT", "5000");
        return overrides;
    }
}
