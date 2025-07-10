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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * VCR mock tests for WeatherService using real OpenWeatherMap API calls.
 * These tests record HTTP interactions to cassettes for replay without API costs.
 *
 * To run these tests:
 * - Set WEATHER_API_KEY environment variable
 * - Run: mvn test -Dtest=WeatherServiceVCRMockTest
 * - For CI/CD: mvn verify -Pvcr-tests (uses replay mode)
 */
final class WeatherServiceVCRMockTest {

    private static final String CASSETTES_PATH = "src/test/resources/cassettes/weather";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final String API_KEY = System.getenv("WEATHER_API_KEY");
    private static final String UNITS = "imperial";
    private static final Pattern API_KEY_PATTERN = Pattern.compile("appid=[^&]*");

    private ObjectMapper objectMapper;
    private AdvancedSettings vcrSettings;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Configure VCR with enhanced censoring for API keys
        vcrSettings = new AdvancedSettings();
        vcrSettings.censors = new Censors()
                .censorQueryParametersByKeys(List.of("appid"))
                .censorHeadersByKeys(List.of("Authorization", "X-API-Key"));
    }

    @AfterEach
    void tearDown() throws Exception {
        // Post-process all cassette files to ensure API keys are censored
        if (API_KEY != null) {
            censorApiKeysInAllCassettes();
        }
    }

    @Test
    @EnabledIf("shouldEnableVcrTests")
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
    @EnabledIf("shouldEnableVcrTests")
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
    @EnabledIf("shouldEnableVcrTests")
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
    @EnabledIf("shouldEnableVcrTests")
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
    @EnabledIf("shouldEnableVcrTests")
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

        final Mode mode = determineVcrMode();
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
        final Mode mode = determineVcrMode();
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

    private Mode determineVcrMode() {
        final String vcrMode = System.getProperty("vcr.mode", "auto");
        return switch (vcrMode.toLowerCase()) {
            case "record" -> Mode.Record;
            case "replay" -> Mode.Replay;
            case "bypass" -> Mode.Bypass;
            default -> Mode.Auto;
        };
    }

    /**
     * Manual URI censoring method to ensure API keys are always masked.
     * This provides a backup to EasyVCR's built-in censoring which can be inconsistent.
     *
     * @param uri the URI string to censor
     * @return the URI with API key replaced with asterisks
     */
    private String censorApiKeyInUri(final String uri) {
        if (uri == null || API_KEY == null) {
            return uri;
        }
        // Replace both the actual API key and the appid parameter value with asterisks
        return API_KEY_PATTERN.matcher(uri).replaceAll("appid=*****");
    }

    /**
     * Post-process all cassette files to ensure API keys are properly censored.
     * This method reads each cassette file and replaces any exposed API keys with asterisks.
     */
    private void censorApiKeysInAllCassettes() throws Exception {
        final Path cassettesDir = Paths.get(CASSETTES_PATH);
        if (!Files.exists(cassettesDir)) {
            return;
        }

        try (var directoryStream = Files.newDirectoryStream(cassettesDir, "*.json")) {
            for (final Path cassetteFile : directoryStream) {
                try {
                    final String originalContent = Files.readString(cassetteFile, StandardCharsets.UTF_8);
                    final String censoredContent = censorApiKeyInContent(originalContent);
                    if (!originalContent.equals(censoredContent)) {
                        Files.writeString(cassetteFile, censoredContent, StandardCharsets.UTF_8);
                    }
                } catch (final Exception e) {
                    throw new RuntimeException("Failed to censor API key in cassette: " + cassetteFile, e);
                }
            }
        }
    }

    /**
     * Censor API keys in the content of a cassette file.
     *
     * @param content the original cassette file content
     * @return the content with API keys replaced with asterisks
     */
    private String censorApiKeyInContent(final String content) {
        if (content == null || API_KEY == null) {
            return content;
        }
        // Replace direct API key occurrences
        String censoredContent = content.replace(API_KEY, "*****");
        // Replace encoded API key occurrences
        final String encodedApiKey = URLEncoder.encode(API_KEY, StandardCharsets.UTF_8);
        censoredContent = censoredContent.replace(encodedApiKey, "*****");
        return censoredContent;
    }

    /**
     * Determines if VCR tests should be enabled.
     * Tests run when:
     * - API key is available (for record/auto modes)
     * - In replay mode (cassettes exist, no API key needed)
     * - In auto mode with existing cassettes (defaults to replay)
     *
     * @return true if tests should be enabled
     */
    static boolean shouldEnableVcrTests() {
        final String apiKey = System.getenv("WEATHER_API_KEY");
        final String vcrMode = System.getProperty("vcr.mode", "auto");

        // Always enable for replay mode (cassettes don't need API key)
        if ("replay".equalsIgnoreCase(vcrMode)) {
            return true;
        }

        // For auto mode, check if cassettes exist (enables replay without API key)
        if ("auto".equalsIgnoreCase(vcrMode) && cassettesExist()) {
            return true;
        }

        // For record mode or auto mode without cassettes, require API key
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * Check if VCR cassettes exist for these tests.
     *
     * @return true if cassette files exist
     */
    private static boolean cassettesExist() {
        final Path cassettesDir = Paths.get(CASSETTES_PATH);
        if (!Files.exists(cassettesDir)) {
            return false;
        }

        // Check for existence of key cassette files
        final String[] cassetteNames = {
            "current_weather_new_york.json",
            "current_weather_london.json",
            "forecast_san_francisco.json",
            "current_weather_tokyo_coordinates.json",
            "current_weather_invalid_location.json"
        };

        for (final String cassetteName : cassetteNames) {
            if (!Files.exists(cassettesDir.resolve(cassetteName))) {
                return false;
            }
        }

        return true;
    }
}
