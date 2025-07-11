package com.jasonriddle.mcp.time;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;

/**
 * Configuration class for Clock dependency injection.
 */
@ApplicationScoped
public final class ClockConfiguration {

    /**
     * Produces a Clock instance for dependency injection.
     *
     * @return system default clock instance.
     */
    @Produces
    @ApplicationScoped
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
