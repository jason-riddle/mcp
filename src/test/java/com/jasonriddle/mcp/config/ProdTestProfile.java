package com.jasonriddle.mcp.config;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Test profile for production configuration validation.
 */
public final class ProdTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "prod";
    }
}
