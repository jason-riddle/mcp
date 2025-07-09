package com.jasonriddle.mcp.time;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Service for time and timezone operations.
 */
@ApplicationScoped
public final class TimeService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final long SECONDS_PER_HOUR = 3600L;

    /**
     * Get current time in specified timezone.
     *
     * @param timezoneName IANA timezone name (e.g., "America/New_York", "Europe/London").
     * @return current time in specified timezone.
     * @throws IllegalArgumentException if timezone is invalid.
     */
    public ZonedDateTime getCurrentTime(final String timezoneName) {
        try {
            final ZoneId timezone = ZoneId.of(timezoneName);
            return ZonedDateTime.now(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezoneName, e);
        }
    }

    /**
     * Convert time between timezones.
     *
     * @param sourceTimezone source IANA timezone name.
     * @param timeString time in 24-hour format (HH:MM).
     * @param targetTimezone target IANA timezone name.
     * @return conversion result with source time, target time, and time difference.
     * @throws IllegalArgumentException if timezone is invalid or time format is invalid.
     */
    public TimeConversionResult convertTime(
            final String sourceTimezone, final String timeString, final String targetTimezone) {

        final ZoneId sourceZone = parseTimezone(sourceTimezone);
        final ZoneId targetZone = parseTimezone(targetTimezone);
        final LocalTime parsedTime = parseTimeString(timeString);

        final LocalDate currentDate = LocalDate.now();
        final ZonedDateTime sourceTime = ZonedDateTime.of(currentDate, parsedTime, sourceZone);
        final ZonedDateTime targetTime = sourceTime.withZoneSameInstant(targetZone);

        final String timeDifference = calculateTimeDifference(sourceTime, targetTime);

        return new TimeConversionResult(sourceTime, targetTime, timeDifference);
    }

    private ZoneId parseTimezone(final String timezoneName) {
        try {
            return ZoneId.of(timezoneName);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezoneName, e);
        }
    }

    private LocalTime parseTimeString(final String timeString) {
        try {
            return LocalTime.parse(timeString, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Expected HH:MM in 24-hour format", e);
        }
    }

    private String calculateTimeDifference(final ZonedDateTime sourceTime, final ZonedDateTime targetTime) {
        final long sourceOffsetSeconds = sourceTime.getOffset().getTotalSeconds();
        final long targetOffsetSeconds = targetTime.getOffset().getTotalSeconds();
        final double hoursDifference = (targetOffsetSeconds - sourceOffsetSeconds) / (double) SECONDS_PER_HOUR;
        return formatTimeDifference(hoursDifference);
    }

    /**
     * Check if the given time is during daylight saving time.
     *
     * @param zonedDateTime time to check.
     * @return true if the time is during daylight saving time.
     */
    public boolean isDaylightSavingTime(final ZonedDateTime zonedDateTime) {
        return zonedDateTime.getZone().getRules().isDaylightSavings(zonedDateTime.toInstant());
    }

    private String formatTimeDifference(final double hoursDifference) {
        if (hoursDifference == (int) hoursDifference) {
            // Whole hours
            return String.format("%+.1fh", hoursDifference);
        } else {
            // Fractional hours (e.g., Nepal's +5.75 hours)
            final String formatted = String.format("%+.2f", hoursDifference);
            return formatted.replaceAll("0+$", "").replaceAll("\\.$", "") + "h";
        }
    }
}
