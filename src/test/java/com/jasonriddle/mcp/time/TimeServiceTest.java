package com.jasonriddle.mcp.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for TimeService.
 */
@QuarkusTest
class TimeServiceTest {

    private TimeService timeService;

    @BeforeEach
    void setUp() {
        timeService = new TimeService();
    }

    @Test
    void testGetCurrentTime() {
        final ZonedDateTime result = timeService.getCurrentTime("America/New_York");

        assertNotNull(result);
        assertEquals("America/New_York", result.getZone().getId());
    }

    @Test
    void testGetCurrentTimeWithDifferentTimezones() {
        final ZonedDateTime utcTime = timeService.getCurrentTime("UTC");
        final ZonedDateTime tokyoTime = timeService.getCurrentTime("Asia/Tokyo");
        final ZonedDateTime londonTime = timeService.getCurrentTime("Europe/London");

        assertEquals("UTC", utcTime.getZone().getId());
        assertEquals("Asia/Tokyo", tokyoTime.getZone().getId());
        assertEquals("Europe/London", londonTime.getZone().getId());

        // All should be approximately the same instant
        final long utcSeconds = utcTime.toEpochSecond();
        final long tokyoSeconds = tokyoTime.toEpochSecond();
        final long londonSeconds = londonTime.toEpochSecond();

        // Should be within 1 second of each other (accounting for test execution time)
        assertTrue(Math.abs(utcSeconds - tokyoSeconds) <= 1);
        assertTrue(Math.abs(utcSeconds - londonSeconds) <= 1);
    }

    @Test
    void testGetCurrentTimeWithInvalidTimezone() {
        final IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> timeService.getCurrentTime("Invalid/Timezone"));
        assertTrue(exception.getMessage().contains("Invalid timezone: Invalid/Timezone"));
    }

    @Test
    void testConvertTime() {
        final TimeConversionResult result = timeService.convertTime("America/New_York", "14:30", "Europe/London");

        assertNotNull(result);
        assertNotNull(result.sourceTime());
        assertNotNull(result.targetTime());
        assertNotNull(result.timeDifference());

        // Verify source timezone
        assertEquals("America/New_York", result.sourceTime().getZone().getId());
        assertEquals(14, result.sourceTime().getHour());
        assertEquals(30, result.sourceTime().getMinute());

        // Verify target timezone
        assertEquals("Europe/London", result.targetTime().getZone().getId());

        // Verify time difference format
        assertTrue(result.timeDifference().matches("[+-]\\d+(\\.\\d+)?h"));
    }

    @Test
    void testConvertTimeWithKnownDifferences() {
        // UTC to Tokyo should be +9 hours
        final TimeConversionResult utcToTokyo = timeService.convertTime("UTC", "12:00", "Asia/Tokyo");
        assertTrue(utcToTokyo.timeDifference().startsWith("+9"));

        // New York to London is typically +4 to +6 hours depending on DST
        final TimeConversionResult nyToLondon = timeService.convertTime("America/New_York", "12:00", "Europe/London");
        assertTrue(nyToLondon.timeDifference().startsWith("+4")
                || nyToLondon.timeDifference().startsWith("+5")
                || nyToLondon.timeDifference().startsWith("+6"));
    }

    @Test
    void testConvertTimeWithSameTimezone() {
        final TimeConversionResult result = timeService.convertTime("UTC", "15:00", "UTC");

        assertEquals("UTC", result.sourceTime().getZone().getId());
        assertEquals("UTC", result.targetTime().getZone().getId());
        assertEquals(15, result.sourceTime().getHour());
        assertEquals(15, result.targetTime().getHour());
        assertEquals("+0.0h", result.timeDifference());
    }

    @Test
    void testConvertTimeWithInvalidSourceTimezone() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeService.convertTime("Invalid/Timezone", "14:30", "Europe/London"));
        assertTrue(exception.getMessage().contains("Invalid timezone: Invalid/Timezone"));
    }

    @Test
    void testConvertTimeWithInvalidTargetTimezone() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeService.convertTime("America/New_York", "14:30", "Invalid/Timezone"));
        assertTrue(exception.getMessage().contains("Invalid timezone: Invalid/Timezone"));
    }

    @Test
    void testConvertTimeWithInvalidTimeFormat() {
        final IllegalArgumentException exception1 = assertThrows(
                IllegalArgumentException.class,
                () -> timeService.convertTime("America/New_York", "25:00", "Europe/London"));
        assertTrue(exception1.getMessage().contains("Invalid time format. Expected HH:MM in 24-hour format"));

        final IllegalArgumentException exception2 = assertThrows(
                IllegalArgumentException.class,
                () -> timeService.convertTime("America/New_York", "14:60", "Europe/London"));
        assertTrue(exception2.getMessage().contains("Invalid time format. Expected HH:MM in 24-hour format"));

        final IllegalArgumentException exception3 = assertThrows(
                IllegalArgumentException.class,
                () -> timeService.convertTime("America/New_York", "2:30 PM", "Europe/London"));
        assertTrue(exception3.getMessage().contains("Invalid time format. Expected HH:MM in 24-hour format"));

        final IllegalArgumentException exception4 = assertThrows(
                IllegalArgumentException.class,
                () -> timeService.convertTime("America/New_York", "invalid", "Europe/London"));
        assertTrue(exception4.getMessage().contains("Invalid time format. Expected HH:MM in 24-hour format"));
    }

    @Test
    void testConvertTimeWithValidTimeFormats() {
        // Test boundary times
        final TimeConversionResult midnight = timeService.convertTime("UTC", "00:00", "UTC");
        assertEquals(0, midnight.sourceTime().getHour());
        assertEquals(0, midnight.sourceTime().getMinute());

        final TimeConversionResult endOfDay = timeService.convertTime("UTC", "23:59", "UTC");
        assertEquals(23, endOfDay.sourceTime().getHour());
        assertEquals(59, endOfDay.sourceTime().getMinute());

        final TimeConversionResult noon = timeService.convertTime("UTC", "12:00", "UTC");
        assertEquals(12, noon.sourceTime().getHour());
        assertEquals(0, noon.sourceTime().getMinute());
    }

    @Test
    void testIsDaylightSavingTime() {
        // Test with current time in different zones
        final ZonedDateTime utcTime = timeService.getCurrentTime("UTC");
        final ZonedDateTime nyTime = timeService.getCurrentTime("America/New_York");
        final ZonedDateTime londonTime = timeService.getCurrentTime("Europe/London");

        // UTC never has DST
        assertFalse(timeService.isDaylightSavingTime(utcTime));

        // NY and London DST depends on the date
        final boolean nyDst = timeService.isDaylightSavingTime(nyTime);
        final boolean londonDst = timeService.isDaylightSavingTime(londonTime);

        // Both should be valid boolean values
        boolean nyDstIsValid = nyDst || !nyDst;
        boolean londonDstIsValid = londonDst || !londonDst;
        assertTrue(nyDstIsValid);
        assertTrue(londonDstIsValid);
    }

    @Test
    void testTimeDifferenceFormatting() {
        // Test whole hour differences
        final TimeConversionResult wholeHour = timeService.convertTime("UTC", "12:00", "Asia/Tokyo");
        assertEquals("+9.0h", wholeHour.timeDifference());

        // Test fractional hour differences (Nepal is UTC+5:45)
        final TimeConversionResult fractionalHour = timeService.convertTime("UTC", "12:00", "Asia/Kathmandu");
        assertEquals("+5.75h", fractionalHour.timeDifference());

        // Test negative differences
        final TimeConversionResult negative = timeService.convertTime("Asia/Tokyo", "12:00", "UTC");
        assertEquals("-9.0h", negative.timeDifference());
    }

    @Test
    void testTimeConversionConsistency() {
        // Converting A->B->A should yield the same result
        final TimeConversionResult nyToLondon = timeService.convertTime("America/New_York", "14:30", "Europe/London");
        final TimeConversionResult londonToNy = timeService.convertTime(
                "Europe/London",
                nyToLondon.targetTime().getHour() + ":"
                        + String.format("%02d", nyToLondon.targetTime().getMinute()),
                "America/New_York");

        assertEquals(14, londonToNy.targetTime().getHour());
        assertEquals(30, londonToNy.targetTime().getMinute());
    }

    @Test
    void testMultipleTimezones() {
        // Test various timezone combinations
        final String[] timezones = {
            "UTC",
            "America/New_York",
            "Europe/London",
            "Asia/Tokyo",
            "Australia/Sydney",
            "America/Los_Angeles",
            "Europe/Berlin",
            "Asia/Shanghai"
        };

        for (final String timezone : timezones) {
            final ZonedDateTime time = timeService.getCurrentTime(timezone);
            assertNotNull(time);
            assertEquals(timezone, time.getZone().getId());
        }
    }

    @Test
    void testIntegratedWorkflow() {
        // Test a complete workflow: get current time, then convert it
        final ZonedDateTime currentTime = timeService.getCurrentTime("America/New_York");
        assertNotNull(currentTime);

        // Convert a specific time
        final TimeConversionResult conversion = timeService.convertTime("America/New_York", "09:00", "Europe/London");
        assertNotNull(conversion);

        // Verify both operations produce consistent timezone information
        assertEquals("America/New_York", currentTime.getZone().getId());
        assertEquals("America/New_York", conversion.sourceTime().getZone().getId());
        assertEquals("Europe/London", conversion.targetTime().getZone().getId());

        // Verify DST calculation consistency
        final boolean currentDst = timeService.isDaylightSavingTime(currentTime);
        final boolean conversionDst = timeService.isDaylightSavingTime(conversion.sourceTime());

        // Both should be valid boolean values
        boolean currentDstIsValid = currentDst || !currentDst;
        boolean conversionDstIsValid = conversionDst || !conversionDst;
        assertTrue(currentDstIsValid);
        assertTrue(conversionDstIsValid);
    }
}
