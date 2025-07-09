package com.jasonriddle.mcp.weather;

/**
 * Weather forecast data record.
 *
 * @param date date in YYYY-MM-DD format
 * @param dayName day name (e.g., Monday)
 * @param highTemp high temperature in Fahrenheit
 * @param lowTemp low temperature in Fahrenheit
 * @param condition weather condition
 * @param description detailed description
 * @param humidity humidity percentage
 * @param windSpeed wind speed in mph
 * @param precipitation precipitation in inches
 */
public record WeatherForecast(
        String date,
        String dayName,
        double highTemp,
        double lowTemp,
        String condition,
        String description,
        int humidity,
        double windSpeed,
        double precipitation) {}
