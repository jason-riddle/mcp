package com.jasonriddle.mcp.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.easypost.easyvcr.AdvancedSettings;
import com.easypost.easyvcr.Cassette;
import com.easypost.easyvcr.Censors;
import com.easypost.easyvcr.Mode;
import com.easypost.easyvcr.clients.httpurlconnection.RecordableHttpsURLConnection;
import com.easypost.easyvcr.clients.httpurlconnection.RecordableURL;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * VCR integration tests for WeatherService using real OpenWeatherMap API calls.
 * These tests record HTTP interactions to cassettes for replay without API costs.
 *
 * To run these tests:
 * - Set WEATHER_API_KEY environment variable
 * - Run: mvn test -Dtest=WeatherServiceVCRIntegrationTest
 * - For CI/CD: mvn verify -Pvcr-tests (uses replay mode)
 */
final class WeatherServiceVCRIntegrationTest {

    private static final String CASSETTES_PATH = "src/test/resources/cassettes";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final String API_KEY = System.getenv("WEATHER_API_KEY");
    private static final String UNITS = "imperial";

    private ObjectMapper objectMapper;
    private AdvancedSettings vcrSettings;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Configure VCR to censor API key
        vcrSettings = new AdvancedSettings();
        vcrSettings.censors = new Censors()
                .censorQueryParametersByKeys(List.of("appid"))
                .censorHeadersByKeys(List.of("Authorization", "X-API-Key"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WEATHER_API_KEY", matches = ".*")
    void testGetCurrentWeatherNewYork() throws Exception {
        // Given
        final String location = "New York,US";
        final Cassette cassette = new Cassette(CASSETTES_PATH, "current_weather_new_york");

        // When
        final Map<String, Object> response = makeCurrentWeatherRequest(location, cassette);

        // Then
        assertNotNull(response);
        assertEquals("New York", response.get("name"));

        final Map<String, Object> main = (Map<String, Object>) response.get("main");
        assertNotNull(main);
        assertTrue(main.containsKey("temp"));
        assertTrue(main.containsKey("humidity"));

        final List<Map<String, Object>> weather = (List<Map<String, Object>>) response.get("weather");
        assertNotNull(weather);
        assertTrue(weather.size() > 0);
        assertTrue(weather.get(0).containsKey("main"));
        assertTrue(weather.get(0).containsKey("description"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WEATHER_API_KEY", matches = ".*")
    void testGetCurrentWeatherLondon() throws Exception {
        // Given
        final String location = "London,UK";
        final Cassette cassette = new Cassette(CASSETTES_PATH, "current_weather_london");

        // When
        final Map<String, Object> response = makeCurrentWeatherRequest(location, cassette);

        // Then
        assertNotNull(response);
        assertEquals("London", response.get("name"));

        final Map<String, Object> main = (Map<String, Object>) response.get("main");
        assertNotNull(main);
        assertTrue(main.containsKey("temp"));
        assertTrue(main.containsKey("feels_like"));
        assertTrue(main.containsKey("humidity"));

        final Map<String, Object> wind = (Map<String, Object>) response.get("wind");
        assertNotNull(wind);
        assertTrue(wind.containsKey("speed"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WEATHER_API_KEY", matches = ".*")
    void testGetForecastSanFrancisco() throws Exception {
        // Given
        final String location = "San Francisco,US";
        final int count = 8; // 3 days worth of 3-hour forecasts
        final Cassette cassette = new Cassette(CASSETTES_PATH, "forecast_san_francisco");

        // When
        final Map<String, Object> response = makeForecastRequest(location, count, cassette);

        // Then
        assertNotNull(response);
        assertTrue(response.containsKey("list"));

        final List<Map<String, Object>> forecasts = (List<Map<String, Object>>) response.get("list");
        assertNotNull(forecasts);
        assertTrue(forecasts.size() > 0);

        // Verify first forecast entry
        final Map<String, Object> firstForecast = forecasts.get(0);
        assertNotNull(firstForecast);
        assertTrue(firstForecast.containsKey("main"));
        assertTrue(firstForecast.containsKey("weather"));
        assertTrue(firstForecast.containsKey("dt"));

        final Map<String, Object> main = (Map<String, Object>) firstForecast.get("main");
        assertTrue(main.containsKey("temp"));
        assertTrue(main.containsKey("humidity"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WEATHER_API_KEY", matches = ".*")
    void testGetCurrentWeatherWithCoordinates() throws Exception {
        // Given - Tokyo coordinates
        final String location = "35.6762,139.6503";
        final Cassette cassette = new Cassette(CASSETTES_PATH, "current_weather_tokyo_coordinates");

        // When
        final Map<String, Object> response = makeCurrentWeatherRequestByCoordinates(location, cassette);

        // Then
        assertNotNull(response);
        assertTrue(response.containsKey("name"));

        final Map<String, Object> coord = (Map<String, Object>) response.get("coord");
        assertNotNull(coord);
        assertTrue(coord.containsKey("lat"));
        assertTrue(coord.containsKey("lon"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WEATHER_API_KEY", matches = ".*")
    void testGetCurrentWeatherWithInvalidLocation() throws Exception {
        // Given
        final String location = "InvalidLocationThatDoesNotExist";
        final Cassette cassette = new Cassette(CASSETTES_PATH, "current_weather_invalid_location");

        // When
        final int responseCode = makeCurrentWeatherRequestForResponseCode(location, cassette);

        // Then
        assertEquals(404, responseCode); // OpenWeatherMap returns 404 for invalid locations
    }

    private Map<String, Object> makeCurrentWeatherRequest(final String location, final Cassette cassette)
            throws Exception {
        final String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        final String url =
                String.format("%s/weather?q=%s&appid=%s&units=%s", BASE_URL, encodedLocation, API_KEY, UNITS);

        return makeHttpRequest(url, cassette);
    }

    private Map<String, Object> makeCurrentWeatherRequestByCoordinates(
            final String coordinates, final Cassette cassette) throws Exception {
        final String[] latLon = coordinates.split(",");
        final String url = String.format(
                "%s/weather?lat=%s&lon=%s&appid=%s&units=%s", BASE_URL, latLon[0], latLon[1], API_KEY, UNITS);

        return makeHttpRequest(url, cassette);
    }

    private Map<String, Object> makeForecastRequest(final String location, final int count, final Cassette cassette)
            throws Exception {
        final String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        final String url = String.format(
                "%s/forecast?q=%s&appid=%s&units=%s&cnt=%d", BASE_URL, encodedLocation, API_KEY, UNITS, count);

        return makeHttpRequest(url, cassette);
    }

    private int makeCurrentWeatherRequestForResponseCode(final String location, final Cassette cassette)
            throws Exception {
        final String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        final String url =
                String.format("%s/weather?q=%s&appid=%s&units=%s", BASE_URL, encodedLocation, API_KEY, UNITS);

        final Mode mode = determineVCRMode();
        final RecordableURL recordableURL = new RecordableURL(url, cassette, mode, vcrSettings);

        final RecordableHttpsURLConnection connection = recordableURL.openConnectionSecure();
        try {
            connection.setRequestMethod("GET");
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    private Map<String, Object> makeHttpRequest(final String url, final Cassette cassette) throws Exception {
        final Mode mode = determineVCRMode();
        final RecordableURL recordableURL = new RecordableURL(url, cassette, mode, vcrSettings);

        final RecordableHttpsURLConnection connection = recordableURL.openConnectionSecure();
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            final int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP request failed with status: " + responseCode);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return objectMapper.readValue(response.toString(), new TypeReference<Map<String, Object>>() {});
        } finally {
            connection.disconnect();
        }
    }

    private Mode determineVCRMode() {
        final String vcrMode = System.getProperty("vcr.mode", "auto");
        return switch (vcrMode.toLowerCase()) {
            case "record" -> Mode.Record;
            case "replay" -> Mode.Replay;
            case "bypass" -> Mode.Bypass;
            default -> Mode.Auto;
        };
    }
}
