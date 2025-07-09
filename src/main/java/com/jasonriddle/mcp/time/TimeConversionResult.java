package com.jasonriddle.mcp.time;

import java.time.ZonedDateTime;

/**
 * Result of time conversion between timezones.
 *
 * @param sourceTime converted source time with timezone.
 * @param targetTime converted target time with timezone.
 * @param timeDifference formatted time difference string (e.g., "+5.0h", "-3.5h").
 */
public record TimeConversionResult(
        ZonedDateTime sourceTime, ZonedDateTime targetTime, String timeDifference) {
}
