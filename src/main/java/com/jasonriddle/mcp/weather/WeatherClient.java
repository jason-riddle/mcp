package com.jasonriddle.mcp.weather;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for OpenWeatherMap API.
 */
@RegisterRestClient(baseUri = "https://api.openweathermap.org")
public interface WeatherClient {

    /**
     * Gets current weather for a location.
     *
     * @param location location name or coordinates
     * @param apiKey OpenWeatherMap API key
     * @param units units for temperature (imperial, metric, kelvin)
     * @return current weather data
     */
    @GET
    @Path("/data/2.5/weather")
    Map<String, Object> getCurrentWeather(
            @QueryParam("q") String location, @QueryParam("appid") String apiKey, @QueryParam("units") String units);

    /**
     * Gets weather forecast for a location.
     *
     * @param location location name or coordinates
     * @param apiKey OpenWeatherMap API key
     * @param units units for temperature (imperial, metric, kelvin)
     * @param count number of forecast entries to return
     * @return forecast data
     */
    @GET
    @Path("/data/2.5/forecast")
    Map<String, Object> getForecast(
            @QueryParam("q") String location,
            @QueryParam("appid") String apiKey,
            @QueryParam("units") String units,
            @QueryParam("cnt") int count);

    /**
     * Gets current weather by coordinates.
     *
     * @param latitude latitude
     * @param longitude longitude
     * @param apiKey OpenWeatherMap API key
     * @param units units for temperature (imperial, metric, kelvin)
     * @return current weather data
     */
    @GET
    @Path("/data/2.5/weather")
    Map<String, Object> getCurrentWeatherByCoords(
            @QueryParam("lat") double latitude,
            @QueryParam("lon") double longitude,
            @QueryParam("appid") String apiKey,
            @QueryParam("units") String units);
}
