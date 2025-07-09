package com.jasonriddle.mcp.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for WeatherService.
 */
final class WeatherServiceTest {

    @Mock
    private WeatherClient weatherClient;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        weatherService = new WeatherService();
        weatherService.weatherClient = weatherClient;
        weatherService.apiKey = "test-api-key";
    }

    @Test
    void testGetCurrentWeather() {
        // Given
        final Map<String, Object> mockResponse = createMockCurrentWeatherResponse();
        when(weatherClient.getCurrentWeather(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        final WeatherData result = weatherService.getCurrentWeather("New York");

        // Then
        assertNotNull(result);
        assertEquals("New York", result.location());
        assertEquals(72.5, result.temperature());
        assertEquals(75.2, result.feelsLike());
        assertEquals(65, result.humidity());
        assertEquals("Clear", result.condition());
        assertEquals("clear sky", result.description());
        assertEquals(5.5, result.windSpeed());
        assertEquals(180, result.windDirection());
        assertEquals(10.0, result.visibility());
        assertNotNull(result.timestamp());

        verify(weatherClient).getCurrentWeather("New York", "test-api-key", "imperial");
    }

    @Test
    void testGetCurrentWeatherWithException() {
        // Given
        final RuntimeException apiError = new RuntimeException("API Error");
        when(weatherClient.getCurrentWeather(anyString(), anyString(), anyString()))
                .thenThrow(apiError);

        // When & Then
        final WeatherService.WeatherServiceException exception = assertThrows(
                WeatherService.WeatherServiceException.class,
                () -> weatherService.getCurrentWeather("Invalid Location"));

        assertTrue(exception.getMessage().contains("Failed to get current weather for: Invalid Location"));
    }

    @Test
    void testGetForecast() {
        // Given
        final Map<String, Object> mockResponse = createMockForecastResponse();
        when(weatherClient.getForecast(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(mockResponse);

        // When
        final List<WeatherForecast> result = weatherService.getForecast("New York", 3);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        final WeatherForecast first = result.get(0);
        assertNotNull(first.date());
        assertNotNull(first.dayName());
        assertEquals(75.0, first.highTemp());
        assertEquals(65.0, first.lowTemp());
        assertEquals("Clouds", first.condition());
        assertEquals("scattered clouds", first.description());
        assertEquals(70, first.humidity());
        assertEquals(7.2, first.windSpeed());
        assertEquals(0.0, first.precipitation());

        verify(weatherClient).getForecast("New York", "test-api-key", "imperial", 24);
    }

    @Test
    void testGetForecastWithException() {
        // Given
        final RuntimeException apiError = new RuntimeException("API Error");
        when(weatherClient.getForecast(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(apiError);

        // When & Then
        final WeatherService.WeatherServiceException exception = assertThrows(
                WeatherService.WeatherServiceException.class, () -> weatherService.getForecast("Invalid Location", 3));

        assertTrue(exception.getMessage().contains("Failed to get forecast for: Invalid Location"));
    }

    @Test
    void testGetWeatherAlertsHeatWarning() {
        // Given
        final Map<String, Object> mockResponse = createMockHeatWarningResponse();

        when(weatherClient.getCurrentWeather(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        final String result = weatherService.getWeatherAlerts("Phoenix");

        // Then
        assertEquals("Heat Warning: Temperature is 98.0°F", result);
    }

    @Test
    void testGetWeatherAlertsColdWarning() {
        // Given
        final Map<String, Object> mockResponse = createMockColdWarningResponse();

        when(weatherClient.getCurrentWeather(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        final String result = weatherService.getWeatherAlerts("Minneapolis");

        // Then
        assertEquals("Cold Warning: Temperature is 20.0°F", result);
    }

    @Test
    void testGetWeatherAlertsWindWarning() {
        // Given
        final Map<String, Object> mockResponse = createMockWindWarningResponse();

        when(weatherClient.getCurrentWeather(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        final String result = weatherService.getWeatherAlerts("Chicago");

        // Then
        assertEquals("Wind Warning: Wind speed is 30.0 mph", result);
    }

    @Test
    void testGetWeatherAlertsNoWarning() {
        // Given
        final Map<String, Object> mockResponse = createMockCurrentWeatherResponse();
        when(weatherClient.getCurrentWeather(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        final String result = weatherService.getWeatherAlerts("San Francisco");

        // Then
        assertEquals("No weather alerts for this location", result);
    }

    @Test
    void testGetWeatherAlertsWithException() {
        // Given
        final RuntimeException apiError = new RuntimeException("API Error");
        when(weatherClient.getCurrentWeather(anyString(), anyString(), anyString()))
                .thenThrow(apiError);

        // When
        final String result = weatherService.getWeatherAlerts("Invalid Location");

        // Then
        assertEquals("Unable to fetch weather alerts for: Invalid Location", result);
    }

    private Map<String, Object> createMockCurrentWeatherResponse() {
        final Map<String, Object> main = Map.of(
                "temp", 72.5,
                "feels_like", 75.2,
                "humidity", 65);

        final Map<String, Object> wind = Map.of("speed", 5.5, "deg", 180);

        final Map<String, Object> weather = Map.of(
                "main", "Clear",
                "description", "clear sky");

        return Map.of(
                "name",
                "New York",
                "main",
                main,
                "wind",
                wind,
                "weather",
                List.of(weather),
                "visibility",
                10000,
                "dt",
                Instant.now().getEpochSecond());
    }

    private Map<String, Object> createMockForecastResponse() {
        final Map<String, Object> forecast1 = Map.of(
                "main",
                        Map.of(
                                "temp_max", 75.0,
                                "temp_min", 65.0,
                                "humidity", 70),
                "wind", Map.of("speed", 7.2),
                "weather",
                        List.of(Map.of(
                                "main", "Clouds",
                                "description", "scattered clouds")),
                "dt", Instant.now().getEpochSecond());

        final Map<String, Object> forecast2 = Map.of(
                "main",
                        Map.of(
                                "temp_max", 78.0,
                                "temp_min", 68.0,
                                "humidity", 75),
                "wind", Map.of("speed", 6.8),
                "weather",
                        List.of(Map.of(
                                "main", "Rain",
                                "description", "light rain")),
                "dt", Instant.now().plusSeconds(86400).getEpochSecond());

        return Map.of("list", List.of(forecast1, forecast2));
    }

    private Map<String, Object> createMockHeatWarningResponse() {
        final Map<String, Object> main = Map.of(
                "temp", 98.0,
                "feels_like", 105.0,
                "humidity", 65);

        final Map<String, Object> wind = Map.of("speed", 5.5, "deg", 180);

        final Map<String, Object> weather = Map.of(
                "main", "Clear",
                "description", "clear sky");

        return Map.of(
                "name",
                "Phoenix",
                "main",
                main,
                "wind",
                wind,
                "weather",
                List.of(weather),
                "visibility",
                10000,
                "dt",
                Instant.now().getEpochSecond());
    }

    private Map<String, Object> createMockColdWarningResponse() {
        final Map<String, Object> main = Map.of(
                "temp", 20.0,
                "feels_like", 15.0,
                "humidity", 65);

        final Map<String, Object> wind = Map.of("speed", 5.5, "deg", 180);

        final Map<String, Object> weather = Map.of(
                "main", "Clear",
                "description", "clear sky");

        return Map.of(
                "name",
                "Minneapolis",
                "main",
                main,
                "wind",
                wind,
                "weather",
                List.of(weather),
                "visibility",
                10000,
                "dt",
                Instant.now().getEpochSecond());
    }

    private Map<String, Object> createMockWindWarningResponse() {
        final Map<String, Object> main = Map.of(
                "temp", 72.5,
                "feels_like", 75.2,
                "humidity", 65);

        final Map<String, Object> wind = Map.of("speed", 30.0, "deg", 180);

        final Map<String, Object> weather = Map.of(
                "main", "Clear",
                "description", "clear sky");

        return Map.of(
                "name",
                "Chicago",
                "main",
                main,
                "wind",
                wind,
                "weather",
                List.of(weather),
                "visibility",
                10000,
                "dt",
                Instant.now().getEpochSecond());
    }
}
