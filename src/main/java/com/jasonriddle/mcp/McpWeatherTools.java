package com.jasonriddle.mcp;

import com.jasonriddle.mcp.weather.WeatherData;
import com.jasonriddle.mcp.weather.WeatherForecast;
import com.jasonriddle.mcp.weather.WeatherService;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP weather tools for OpenWeatherMap integration.
 */
@ApplicationScoped
public final class McpWeatherTools {

    @Inject
    WeatherService weatherService;

    /**
     * Gets current weather for a location.
     *
     * @param location city name or coordinates.
     * @return current weather data as map.
     */
    @Tool(name = "weather.current", description = "Get current weather for a location")
    public Map<String, Object> getCurrentWeather(
            @ToolArg(description = "City name (e.g., 'New York') or coordinates (lat,lon)") final String location) {

        try {
            final WeatherData weather = weatherService.getCurrentWeather(location);

            return Map.of(
                    "location", weather.location(),
                    "temperature", weather.temperature(),
                    "condition", weather.condition(),
                    "description", weather.description(),
                    "timestamp", weather.timestamp().toString());
        } catch (final WeatherService.WeatherServiceException e) {
            return Map.of("error", "Unable to fetch current weather: " + e.getMessage());
        }
    }

    /**
     * Gets weather forecast for a location.
     *
     * @param location city name or coordinates.
     * @param days number of days to forecast.
     * @return list of forecast data as maps.
     */
    @Tool(name = "weather.forecast", description = "Get weather forecast for a location")
    public List<Map<String, Object>> getWeatherForecast(
            @ToolArg(description = "City name or coordinates") final String location,
            @ToolArg(description = "Number of days (1-5)") final int days) {

        try {
            final List<WeatherForecast> forecasts = weatherService.getForecast(location, days);

            final List<Map<String, Object>> result = new ArrayList<>();
            for (final WeatherForecast forecast : forecasts) {
                final Map<String, Object> forecastMap = Map.of(
                        "date", forecast.date(),
                        "high_temp", forecast.highTemp(),
                        "low_temp", forecast.lowTemp(),
                        "condition", forecast.condition(),
                        "description", forecast.description());
                result.add(forecastMap);
            }
            return result;
        } catch (final WeatherService.WeatherServiceException e) {
            return List.of(Map.of("error", "Unable to fetch forecast: " + e.getMessage()));
        }
    }

    /**
     * Gets weather alerts for a location.
     *
     * @param location city name or coordinates.
     * @return formatted weather alerts.
     */
    @Tool(name = "weather.alerts", description = "Get weather alerts for a location")
    public String getWeatherAlerts(@ToolArg(description = "City name or coordinates") final String location) {

        return weatherService.getWeatherAlerts(location);
    }
}
