package com.jasonriddle.mcp.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Jazzer fuzzing harness for {@link TimeService}.
 */
final class TimeServiceFuzzTest {

    private final TimeService timeService = new TimeService();

    /**
     * Fuzz the {@link TimeService#getCurrentTime(String)} method.
     *
     * @param data supplied fuzzing data
     */
    @FuzzTest
    void fuzzGetCurrentTime(final FuzzedDataProvider data) {
        final String timezone = data.consumeString(50);
        try {
            timeService.getCurrentTime(timezone);
        } catch (IllegalArgumentException ignored) {
            // Expected for invalid timezones
        }
    }

    /**
     * Fuzz the {@link TimeService#convertTime(String, String, String)} method.
     *
     * @param data supplied fuzzing data
     */
    @FuzzTest
    void fuzzConvertTime(final FuzzedDataProvider data) {
        final String source = data.consumeString(50);
        final String time = data.consumeString(8);
        final String target = data.consumeRemainingAsString();
        try {
            timeService.convertTime(source, time, target);
        } catch (IllegalArgumentException ignored) {
            // Expected for invalid inputs
        }
    }

    /**
     * Fuzz the {@link TimeService#isDaylightSavingTime(ZonedDateTime)} method.
     *
     * @param data supplied fuzzing data
     */
    @FuzzTest
    void fuzzIsDst(final FuzzedDataProvider data) {
        final String zoneName = data.consumeString(50);
        try {
            final ZoneId zone = ZoneId.of(zoneName);
            final ZonedDateTime time = ZonedDateTime.now(zone);
            timeService.isDaylightSavingTime(time);
        } catch (Exception ignored) {
            // Ignore invalid zones
        }
    }
}
