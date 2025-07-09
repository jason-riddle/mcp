package com.jasonriddle.mcp;

import com.jasonriddle.mcp.time.TimeConversionResult;
import com.jasonriddle.mcp.time.TimeService;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * MCP tools for time and timezone operations.
 */
@ApplicationScoped
public final class McpTimeTools {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String TIMEZONE_KEY = "timezone";
    private static final String DATETIME_KEY = "datetime";
    private static final String IS_DST_KEY = "is_dst";

    @Inject
    TimeService timeService;

    /**
     * Get current time in a specific timezone.
     *
     * @param timezone IANA timezone name (e.g., 'America/New_York', 'Europe/London').
     * @return time result containing timezone, datetime, and DST status.
     */
    @Tool(name = "get_current_time", description = "Get current time in a specific timezone")
    public Map<String, Object> getCurrentTime(
            @ToolArg(description = "IANA timezone name (e.g., 'America/New_York', 'Europe/London')")
                    final String timezone) {
        final ZonedDateTime currentTime = timeService.getCurrentTime(timezone);

        return Map.of(
                TIMEZONE_KEY, timezone,
                DATETIME_KEY, currentTime.format(ISO_FORMATTER),
                IS_DST_KEY, timeService.isDaylightSavingTime(currentTime));
    }

    /**
     * Convert time between timezones.
     *
     * @param sourceTimezone source IANA timezone name.
     * @param time time to convert in 24-hour format (HH:MM).
     * @param targetTimezone target IANA timezone name.
     * @return conversion result with source, target, and time difference.
     */
    @Tool(name = "convert_time", description = "Convert time between timezones")
    public Map<String, Object> convertTime(
            @ToolArg(description = "Source IANA timezone name") final String sourceTimezone,
            @ToolArg(description = "Time to convert in 24-hour format (HH:MM)") final String time,
            @ToolArg(description = "Target IANA timezone name") final String targetTimezone) {
        final TimeConversionResult conversionResult = timeService.convertTime(sourceTimezone, time, targetTimezone);

        final Map<String, Object> sourceInfo = Map.of(
                TIMEZONE_KEY, sourceTimezone,
                DATETIME_KEY, conversionResult.sourceTime().format(ISO_FORMATTER),
                IS_DST_KEY, timeService.isDaylightSavingTime(conversionResult.sourceTime()));

        final Map<String, Object> targetInfo = Map.of(
                TIMEZONE_KEY, targetTimezone,
                DATETIME_KEY, conversionResult.targetTime().format(ISO_FORMATTER),
                IS_DST_KEY, timeService.isDaylightSavingTime(conversionResult.targetTime()));

        return Map.of(
                "source", sourceInfo,
                "target", targetInfo,
                "time_difference", conversionResult.timeDifference());
    }
}
