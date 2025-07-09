package com.jasonriddle.mcp.weather;

import java.time.Instant;

/**
 * Current weather data record.
 *
 * @param location location name
 * @param temperature temperature in Fahrenheit
 * @param feelsLike feels like temperature in Fahrenheit
 * @param humidity humidity percentage
 * @param condition weather condition
 * @param description detailed description
 * @param windSpeed wind speed in mph
 * @param windDirection wind direction in degrees
 * @param visibility visibility in kilometers
 * @param timestamp timestamp of the data
 */
public record WeatherData(
        String location,
        double temperature,
        double feelsLike,
        int humidity,
        String condition,
        String description,
        double windSpeed,
        int windDirection,
        double visibility,
        Instant timestamp) {}
