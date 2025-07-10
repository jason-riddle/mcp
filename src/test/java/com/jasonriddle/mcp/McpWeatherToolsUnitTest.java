package com.jasonriddle.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jasonriddle.mcp.weather.WeatherData;
import com.jasonriddle.mcp.weather.WeatherForecast;
import com.jasonriddle.mcp.weather.WeatherService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for McpWeatherTools MCP integration.
 */
final class McpWeatherToolsUnitTest {

    @Mock
    private WeatherService weatherService;

    private McpWeatherTools mcpWeatherTools;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mcpWeatherTools = new McpWeatherTools();
        mcpWeatherTools.weatherService = weatherService;
    }

    @Test
    void testGetCurrentWeather() {
        // Given
        final WeatherData mockWeatherData =
                new WeatherData("New York", 72.5, 75.2, 65, "Clear", "clear sky", 5.5, 180, 10.0, Instant.now());
        when(weatherService.getCurrentWeather(anyString())).thenReturn(mockWeatherData);

        // When
        final Map<String, Object> result = mcpWeatherTools.getCurrentWeather("New York");

        // Then
        assertNotNull(result);
        assertEquals("New York", result.get("location"));
        assertEquals(72.5, result.get("temperature"));
        assertEquals("Clear", result.get("condition"));
        assertEquals("clear sky", result.get("description"));
        assertTrue(result.containsKey("timestamp"));

        verify(weatherService).getCurrentWeather("New York");
    }

    @Test
    void testGetCurrentWeatherWithException() {
        // Given
        when(weatherService.getCurrentWeather(anyString()))
                .thenThrow(new WeatherService.WeatherServiceException("API Error", new RuntimeException()));

        // When
        final Map<String, Object> result = mcpWeatherTools.getCurrentWeather("Invalid Location");

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        final String error = (String) result.get("error");
        assertTrue(error.contains("Unable to fetch current weather"));
        assertTrue(error.contains("API Error"));
    }

    @Test
    void testGetWeatherForecast() {
        // Given
        final List<WeatherForecast> mockForecasts = List.of(
                new WeatherForecast("2024-01-15", "Monday", 75.0, 65.0, "Clouds", "scattered clouds", 70, 7.2, 0.0),
                new WeatherForecast("2024-01-16", "Tuesday", 78.0, 68.0, "Rain", "light rain", 75, 6.8, 0.25));
        when(weatherService.getForecast(anyString(), anyInt())).thenReturn(mockForecasts);

        // When
        final List<Map<String, Object>> result = mcpWeatherTools.getWeatherForecast("New York", 2);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        final Map<String, Object> first = result.get(0);
        assertEquals("2024-01-15", first.get("date"));
        assertEquals(75.0, first.get("high_temp"));
        assertEquals(65.0, first.get("low_temp"));
        assertEquals("Clouds", first.get("condition"));
        assertEquals("scattered clouds", first.get("description"));

        final Map<String, Object> second = result.get(1);
        assertEquals("2024-01-16", second.get("date"));
        assertEquals(78.0, second.get("high_temp"));
        assertEquals(68.0, second.get("low_temp"));
        assertEquals("Rain", second.get("condition"));
        assertEquals("light rain", second.get("description"));

        verify(weatherService).getForecast("New York", 2);
    }

    @Test
    void testGetWeatherForecastWithException() {
        // Given
        when(weatherService.getForecast(anyString(), anyInt()))
                .thenThrow(new WeatherService.WeatherServiceException("API Error", new RuntimeException()));

        // When
        final List<Map<String, Object>> result = mcpWeatherTools.getWeatherForecast("Invalid Location", 3);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        final Map<String, Object> errorResult = result.get(0);
        assertTrue(errorResult.containsKey("error"));
        final String error = (String) errorResult.get("error");
        assertTrue(error.contains("Unable to fetch forecast"));
        assertTrue(error.contains("API Error"));
    }

    @Test
    void testGetWeatherAlerts() {
        // Given
        final String mockAlerts = "Heat Warning: Temperature is 98.0°F";
        when(weatherService.getWeatherAlerts(anyString())).thenReturn(mockAlerts);

        // When
        final String result = mcpWeatherTools.getWeatherAlerts("Phoenix");

        // Then
        assertEquals("Heat Warning: Temperature is 98.0°F", result);
        verify(weatherService).getWeatherAlerts("Phoenix");
    }

    @Test
    void testGetWeatherAlertsNoWarning() {
        // Given
        final String mockAlerts = "No weather alerts for this location";
        when(weatherService.getWeatherAlerts(anyString())).thenReturn(mockAlerts);

        // When
        final String result = mcpWeatherTools.getWeatherAlerts("San Francisco");

        // Then
        assertEquals("No weather alerts for this location", result);
        verify(weatherService).getWeatherAlerts("San Francisco");
    }

    @Test
    void testGetWeatherAlertsWithError() {
        // Given
        final String mockAlerts = "Unable to fetch weather alerts for: Invalid Location";
        when(weatherService.getWeatherAlerts(anyString())).thenReturn(mockAlerts);

        // When
        final String result = mcpWeatherTools.getWeatherAlerts("Invalid Location");

        // Then
        assertEquals("Unable to fetch weather alerts for: Invalid Location", result);
        verify(weatherService).getWeatherAlerts("Invalid Location");
    }
}
