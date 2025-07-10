package com.jasonriddle.mcp.weather;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Service for weather operations using OpenWeatherMap API.
 */
@ApplicationScoped
public final class WeatherService {

    private static final Logger LOGGER = Logger.getLogger(WeatherService.class.getName());
    private static final String DEFAULT_UNITS = "imperial"; // Fahrenheit
    private static final String MAIN_FIELD = "main";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE");

    // Alert thresholds
    private static final double HEAT_WARNING_THRESHOLD_F = 95.0;
    private static final double COLD_WARNING_THRESHOLD_F = 32.0;
    private static final double WIND_WARNING_THRESHOLD_MPH = 25.0;

    @Inject
    @ConfigProperty(name = "weather.api.key", defaultValue = "")
    String apiKey;

    @Inject
    @RestClient
    WeatherClient weatherClient;

    /**
     * Initialize the weather service and log configuration status.
     */
    @PostConstruct
    void initialize() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOGGER.warning("Weather API key is not configured. Set WEATHER_API_KEY environment "
                    + "variable to enable weather features.");
        } else {
            LOGGER.info("Weather service initialized with API key");
        }
    }

    /**
     * Gets current weather for a location.
     *
     * @param location city name or coordinates.
     * @return current weather data.
     */
    public WeatherData getCurrentWeather(final String location) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new WeatherServiceException(
                    "Weather API key is not configured. Set WEATHER_API_KEY environment variable.");
        }

        try {
            final Map<String, Object> response = weatherClient.getCurrentWeather(location, apiKey, DEFAULT_UNITS);

            return parseWeatherData(response);
        } catch (final Exception e) {
            throw new WeatherServiceException("Failed to get current weather for: " + location, e);
        }
    }

    /**
     * Gets weather forecast for a location.
     *
     * @param location city name or coordinates.
     * @param days number of days to forecast.
     * @return list of forecast data.
     */
    public List<WeatherForecast> getForecast(final String location, final int days) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new WeatherServiceException(
                    "Weather API key is not configured. Set WEATHER_API_KEY environment variable.");
        }

        try {
            final int count = Math.min(days * 8, 40); // 8 forecasts per day, max 40
            final Map<String, Object> response = weatherClient.getForecast(location, apiKey, DEFAULT_UNITS, count);

            return parseForecastData(response, days);
        } catch (final Exception e) {
            throw new WeatherServiceException("Failed to get forecast for: " + location, e);
        }
    }

    /**
     * Gets weather alerts for a location.
     *
     * @param location city name or coordinates.
     * @return formatted weather alerts.
     */
    public String getWeatherAlerts(final String location) {
        // OpenWeatherMap alerts require One Call API (paid)
        // For now, return basic alert info from current weather
        try {
            final WeatherData weather = getCurrentWeather(location);
            return formatBasicAlert(weather);
        } catch (final Exception e) {
            return "Unable to fetch weather alerts for: " + location;
        }
    }

    private WeatherData parseWeatherData(final Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> main = (Map<String, Object>) response.get(MAIN_FIELD);
        @SuppressWarnings("unchecked")
        final Map<String, Object> wind = (Map<String, Object>) response.get("wind");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> weather = (List<Map<String, Object>>) response.get("weather");
        @SuppressWarnings("unchecked")
        final Map<String, Object> weatherInfo = weather.get(0);

        final String locationName = (String) response.get("name");
        final double temperature = ((Number) main.get("temp")).doubleValue();
        final double feelsLike = ((Number) main.get("feels_like")).doubleValue();
        final int humidity = ((Number) main.get("humidity")).intValue();
        final String condition = (String) weatherInfo.get(MAIN_FIELD);
        final String description = (String) weatherInfo.get("description");
        final double windSpeed;
        if (wind != null) {
            windSpeed = ((Number) wind.get("speed")).doubleValue();
        } else {
            windSpeed = 0.0;
        }

        final int windDirection;
        if (wind != null && wind.get("deg") != null) {
            windDirection = ((Number) wind.get("deg")).intValue();
        } else {
            windDirection = 0;
        }

        final double visibility;
        if (response.get("visibility") != null) {
            visibility = ((Number) response.get("visibility")).doubleValue() / 1000.0; // Convert to km
        } else {
            visibility = 0.0;
        }
        final long timestamp = ((Number) response.get("dt")).longValue();

        return new WeatherData(
                locationName,
                temperature,
                feelsLike,
                humidity,
                condition,
                description,
                windSpeed,
                windDirection,
                visibility,
                Instant.ofEpochSecond(timestamp));
    }

    private List<WeatherForecast> parseForecastData(final Map<String, Object> response, final int days) {
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> list = (List<Map<String, Object>>) response.get("list");

        final List<WeatherForecast> forecasts = new ArrayList<>();
        final int limit = days * 8; // 8 forecasts per day
        int count = 0;

        for (final Map<String, Object> entry : list) {
            if (count >= limit) {
                break;
            }
            forecasts.add(parseForecastEntry(entry));
            count++;
        }

        return forecasts;
    }

    private WeatherForecast parseForecastEntry(final Map<String, Object> entry) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> main = (Map<String, Object>) entry.get(MAIN_FIELD);
        @SuppressWarnings("unchecked")
        final Map<String, Object> wind = (Map<String, Object>) entry.get("wind");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> weather = (List<Map<String, Object>>) entry.get("weather");
        @SuppressWarnings("unchecked")
        final Map<String, Object> weatherInfo = weather.get(0);

        final long timestamp = ((Number) entry.get("dt")).longValue();
        final LocalDateTime dateTime =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());

        final double highTemp = ((Number) main.get("temp_max")).doubleValue();
        final double lowTemp = ((Number) main.get("temp_min")).doubleValue();
        final String condition = (String) weatherInfo.get(MAIN_FIELD);
        final String description = (String) weatherInfo.get("description");
        final int humidity = ((Number) main.get("humidity")).intValue();
        final double windSpeed = ((Number) wind.get("speed")).doubleValue();
        final double precipitation = entry.get("rain") != null
                ? ((Number) ((Map<String, Object>) entry.get("rain")).get("3h")).doubleValue()
                : 0.0;

        return new WeatherForecast(
                dateTime.format(DATE_FORMATTER),
                dateTime.format(DAY_FORMATTER),
                highTemp,
                lowTemp,
                condition,
                description,
                humidity,
                windSpeed,
                precipitation);
    }

    private String formatBasicAlert(final WeatherData weather) {
        if (weather.temperature() > HEAT_WARNING_THRESHOLD_F) {
            return "Heat Warning: Temperature is " + weather.temperature() + "°F";
        } else if (weather.temperature() < COLD_WARNING_THRESHOLD_F) {
            return "Cold Warning: Temperature is " + weather.temperature() + "°F";
        } else if (weather.windSpeed() > WIND_WARNING_THRESHOLD_MPH) {
            return "Wind Warning: Wind speed is " + weather.windSpeed() + " mph";
        }
        return "No weather alerts for this location";
    }

    /**
     * Exception for weather service errors.
     */
    public static final class WeatherServiceException extends RuntimeException {
        /**
         * Creates a new weather service exception.
         *
         * @param message error message.
         */
        public WeatherServiceException(final String message) {
            super(message);
        }

        /**
         * Creates a new weather service exception.
         *
         * @param message error message.
         * @param cause underlying cause.
         */
        public WeatherServiceException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
