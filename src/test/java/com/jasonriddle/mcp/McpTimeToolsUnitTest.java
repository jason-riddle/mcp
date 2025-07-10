package com.jasonriddle.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jasonriddle.mcp.time.TimeService;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for McpTimeTools MCP integration.
 */
final class McpTimeToolsUnitTest {

    private TimeService timeService;
    private McpTimeTools mcpTimeTools;

    @BeforeEach
    void setUp() {
        timeService = new TimeService();
        mcpTimeTools = new McpTimeTools();
        mcpTimeTools.timeService = timeService;
    }

    @Test
    void testGetCurrentTime() {
        final Map<String, Object> result = mcpTimeTools.getCurrentTime("America/New_York");

        assertNotNull(result);
        assertEquals("America/New_York", result.get("timezone"));
        assertTrue(result.containsKey("datetime"));
        assertTrue(result.containsKey("is_dst"));

        // Verify datetime format
        final String datetime = (String) result.get("datetime");
        assertNotNull(datetime);
        assertTrue(datetime.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([+-]\\d{2}:\\d{2}|Z)"));
    }

    @Test
    void testGetCurrentTimeWithDifferentTimezones() {
        final Map<String, Object> utcResult = mcpTimeTools.getCurrentTime("UTC");
        final Map<String, Object> tokyoResult = mcpTimeTools.getCurrentTime("Asia/Tokyo");
        final Map<String, Object> londonResult = mcpTimeTools.getCurrentTime("Europe/London");

        assertEquals("UTC", utcResult.get("timezone"));
        assertEquals("Asia/Tokyo", tokyoResult.get("timezone"));
        assertEquals("Europe/London", londonResult.get("timezone"));

        // All should have datetime and is_dst fields
        assertTrue(utcResult.containsKey("datetime"));
        assertTrue(tokyoResult.containsKey("datetime"));
        assertTrue(londonResult.containsKey("datetime"));
        assertTrue(utcResult.containsKey("is_dst"));
        assertTrue(tokyoResult.containsKey("is_dst"));
        assertTrue(londonResult.containsKey("is_dst"));
    }

    @Test
    void testGetCurrentTimeWithInvalidTimezone() {
        assertThrows(IllegalArgumentException.class, () -> mcpTimeTools.getCurrentTime("Invalid/Timezone"));
    }

    @Test
    void testConvertTime() {
        final Map<String, Object> result = mcpTimeTools.convertTime("America/New_York", "14:30", "Europe/London");

        assertNotNull(result);
        assertTrue(result.containsKey("source"));
        assertTrue(result.containsKey("target"));
        assertTrue(result.containsKey("time_difference"));

        // Verify source structure
        @SuppressWarnings("unchecked")
        final Map<String, Object> source = (Map<String, Object>) result.get("source");
        assertEquals("America/New_York", source.get("timezone"));
        assertTrue(source.containsKey("datetime"));
        assertTrue(source.containsKey("is_dst"));

        // Verify target structure
        @SuppressWarnings("unchecked")
        final Map<String, Object> target = (Map<String, Object>) result.get("target");
        assertEquals("Europe/London", target.get("timezone"));
        assertTrue(target.containsKey("datetime"));
        assertTrue(target.containsKey("is_dst"));

        // Verify time difference format
        final String timeDifference = (String) result.get("time_difference");
        assertNotNull(timeDifference);
        assertTrue(timeDifference.matches("[+-]\\d+(\\.\\d+)?h"));
    }

    @Test
    void testConvertTimeWithSpecificScenarios() {
        // Test UTC to Tokyo (should be +9 hours)
        final Map<String, Object> utcToTokyo = mcpTimeTools.convertTime("UTC", "12:00", "Asia/Tokyo");
        final String timeDiff = (String) utcToTokyo.get("time_difference");
        assertTrue(timeDiff.startsWith("+9"));

        // Test same timezone conversion
        final Map<String, Object> sameTimezone = mcpTimeTools.convertTime("UTC", "15:00", "UTC");
        assertEquals("+0.0h", sameTimezone.get("time_difference"));
    }

    @Test
    void testConvertTimeWithInvalidTimezone() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mcpTimeTools.convertTime("Invalid/Timezone", "14:30", "Europe/London"));
        assertThrows(
                IllegalArgumentException.class,
                () -> mcpTimeTools.convertTime("America/New_York", "14:30", "Invalid/Timezone"));
    }

    @Test
    void testConvertTimeWithInvalidTimeFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mcpTimeTools.convertTime("America/New_York", "25:00", "Europe/London"));
        assertThrows(
                IllegalArgumentException.class,
                () -> mcpTimeTools.convertTime("America/New_York", "14:60", "Europe/London"));
        assertThrows(
                IllegalArgumentException.class,
                () -> mcpTimeTools.convertTime("America/New_York", "2:30 PM", "Europe/London"));
        assertThrows(
                IllegalArgumentException.class,
                () -> mcpTimeTools.convertTime("America/New_York", "invalid", "Europe/London"));
    }

    @Test
    void testConvertTimeWithValidTimeFormats() {
        // Test various valid time formats
        final Map<String, Object> result1 = mcpTimeTools.convertTime("UTC", "00:00", "UTC");
        assertNotNull(result1);

        final Map<String, Object> result2 = mcpTimeTools.convertTime("UTC", "23:59", "UTC");
        assertNotNull(result2);

        final Map<String, Object> result3 = mcpTimeTools.convertTime("UTC", "12:30", "UTC");
        assertNotNull(result3);
    }

    @Test
    void testTimeDifferenceCalculation() {
        // Test known timezone differences
        final Map<String, Object> eastToWest = mcpTimeTools.convertTime("Asia/Tokyo", "12:00", "America/New_York");
        final String eastWestDiff = (String) eastToWest.get("time_difference");
        assertTrue(eastWestDiff.startsWith("-"));

        final Map<String, Object> westToEast = mcpTimeTools.convertTime("America/New_York", "12:00", "Asia/Tokyo");
        final String westEastDiff = (String) westToEast.get("time_difference");
        assertTrue(westEastDiff.startsWith("+"));
    }

    @Test
    void testDateTimeFormatting() {
        final Map<String, Object> result = mcpTimeTools.getCurrentTime("UTC");
        final String datetime = (String) result.get("datetime");

        // Verify it can be parsed back
        final ZonedDateTime parsed = ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertNotNull(parsed);
        assertTrue(parsed.getZone().getId().equals("UTC")
                || parsed.getZone().getId().equals("Z"));
    }

    @Test
    void testIntegratedWorkflow() {
        // Test a complete workflow: get current time, then convert it
        final Map<String, Object> currentTime = mcpTimeTools.getCurrentTime("America/New_York");
        assertNotNull(currentTime);

        // Convert a specific time
        final Map<String, Object> conversion = mcpTimeTools.convertTime("America/New_York", "09:00", "Europe/London");
        assertNotNull(conversion);

        // Verify both operations produce consistent timezone information
        assertEquals("America/New_York", currentTime.get("timezone"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> source = (Map<String, Object>) conversion.get("source");
        assertEquals("America/New_York", source.get("timezone"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> target = (Map<String, Object>) conversion.get("target");
        assertEquals("Europe/London", target.get("timezone"));
    }
}
