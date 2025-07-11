Title: Implementing a MCP server in Quarkus

URL Source: https://quarkus.io/blog/mcp-server/

Markdown Content:
The Model Context Protocol (MCP) is an emerging standard that enables AI models to safely interact with external tools and resources. In this tutorial, I’ll show you how to implement an MCP server using Quarkus, allowing you to extend AI applications with custom tools powered by the Java ecosystem.

[](https://quarkus.io/blog/mcp-server/#what-well-be-building)What we’ll be building
-----------------------------------------------------------------------------------

We’ll implement a simple MCP server that provides tools to get weather forecasts and alerts for US-based locations. We’ve chosen this example because it aligns with the official MCP quickstart guide at [modelcontextprotocol.io/quickstart/server](https://modelcontextprotocol.io/quickstart/server), making it easier to compare implementations across different languages.

Our server will expose two tools: `getAlerts` and `getForecast`. Once built, we’ll connect it to an MCP host that runs the server as a subprocess. Here’s how it looks when integrated with Claude:

![Image 1: Claude MCP Integration Example](https://quarkus.io/assets/images/posts/mcp/claude-example.png)

[](https://quarkus.io/blog/mcp-server/#core-mcp-concepts)Core MCP Concepts
--------------------------------------------------------------------------

MCP servers can provide three main types of capabilities:

Resources
File-like data that can be read by clients (like API responses or file contents)

Tools
Functions that can be called by the LLM (with user approval)

Prompts
Pre-written templates that help users accomplish specific tasks

This tutorial focuses on implementing tools.

### [](https://quarkus.io/blog/mcp-server/#prerequisites)Prerequisites

To follow this tutorial you need:

*   Familiarity with Quarkus and Java

*   Understanding of LLMs (OpenAI, Granite, Anthropic, Google, etc.)

### [](https://quarkus.io/blog/mcp-server/#system-requirements)System requirements

*   Quarkus CLI

*   JBang (optional)

### [](https://quarkus.io/blog/mcp-server/#set-up-your-project)Set up your project

First, create a new Quarkus project with rest-client, qute and mcp server extension without default boilerplate code:

`quarkus create app --no-code -x rest-client-jackson,qute,mcp-server-stdio weather`

We’re using the `stdio` variant as it’s required for MCP hosts that run the server as a subprocess. While an `sse` variant exists for Server-Sent Events streaming, we’ll focus on the standard input/output approach.

[](https://quarkus.io/blog/mcp-server/#building-the-server)Building the server
------------------------------------------------------------------------------

Create a new file `src/main/java/org/acme/Weather.java`. The complete code for this example is available [here](https://github.com/quarkiverse/quarkus-mcp-server/tree/main/samples/weather).

### [](https://quarkus.io/blog/mcp-server/#weather-api-integration)Weather API Integration

First, let’s set up the REST client for the weather API:

```
@RegisterRestClient(baseUri = "https://api.weather.gov")
public interface WeatherClient {
    // Get active alerts for a specific state
    @GET
    @Path("/alerts/active/area/{state}")
    Alerts getAlerts(@RestPath String state);

    // Get point metadata for coordinates
    @GET
    @Path("/points/{latitude},{longitude}")
    JsonObject getPoints(@RestPath double latitude, @RestPath double longitude);

    // Get detailed forecast using dynamically provided URL
    @GET
    @Path("/")
    Forecast getForecast(@Url String url);
}
```

To handle the API responses, we’ll define some data classes. Note that we’re only including the fields we need, as the complete API response contains much more data:

```
static record Period(
    String name,
    int temperature,
    String temperatureUnit,
    String windSpeed,
    String windDirection,
    String detailedForecast) {
}

static record ForecastProperties(
        List<Period> periods) {
}

static record Forecast(
        ForecastProperties properties) {
}
```

Since the Weather API uses redirects, add this to your `application.properties`:

`quarkus.rest-client.follow-redirects=true`

### [](https://quarkus.io/blog/mcp-server/#formatting-helpers)Formatting Helpers

We’ll use Qute templates to format the weather data:

```
String formatForecast(Forecast forecast) {
    return forecast.properties().periods().stream().map(period -> {
            // Template for each forecast period
            return Qute.fmt(
                """
                        Temperature: {p.temperature}°{p.temperatureUnit}
                        Wind: {p.windSpeed} {p.windDirection}
                        Forecast: {p.detailedForecast}
                        """,
                Map.of("p", period)).toString();
        }).collect(Collectors.joining("\n---\n"));
    }
```

### [](https://quarkus.io/blog/mcp-server/#implementing-mcp-tools)Implementing MCP Tools

Now let’s implement the actual MCP tools. The `@Tool` annotation from `io.quarkiverse.mcp.server` marks methods as available tools, while `@ToolArg` describes the parameters:

```
@Tool(description = "Get weather alerts for a US state.")
String getAlerts(@ToolArg(description = "Two-letter US state code (e.g. CA, NY)") String state) {
    return formatAlerts(weatherClient.getAlerts(state));
}

@Tool(description = "Get weather forecast for a location.")
String getForecast(
    @ToolArg(description = "Latitude of the location") double latitude,
    @ToolArg(description = "Longitude of the location") double longitude) {

    // First get the point metadata which contains the forecast URL
    var points = weatherClient.getPoints(latitude, longitude);
    // Extract the forecast URL using Qute template
    var url = Qute.fmt("{p.properties.forecast}", Map.of("p", points));
    // Get and format the forecast
    return formatForecast(weatherClient.getForecast(url));
}
```

The forecast API requires a two-step process where we first get point metadata and then use a URL from that response to fetch the actual forecast.

[](https://quarkus.io/blog/mcp-server/#running-the-server)Running the Server
----------------------------------------------------------------------------

To simplify deployment and development, we’ll package the server as an uber-jar. This makes it possible to `mvn install` and publish as a jar to a Maven repository which makes it easiier to share and run for us and others.

`quarkus.package.uber-jar=true`

Finally, we can optionally enable file logging as without it we would not be able to see any logs from the server as standard input/output is reserved for the MCP protocol.

```
quarkus.log.file.enable=true
quarkus.log.file.path=weather-quarkus.log
```

After running `mvn install`, you can use JBang to run the server using its Maven coordinates: `org.acme:weather:1.0.0-SNAPSHOT:runner` or manually using `java -jar target/weather-1.0.0-SNAPSHOT-runner.jar`.

### [](https://quarkus.io/blog/mcp-server/#integration-with-claude-desktop)Integration with Claude Desktop

Add this to your `claude_desktop_config.json`:

```
{
    "mcpServers": {
        "weather": {
            "command": "jbang",
            "args": ["--quiet",
                    "org.acme:weather:1.0.0-SNAPSHOT:runner"]
        }
    }
}
```

The `--quiet` flag prevents JBang’s output from interfering with the MCP protocol.

![Image 2: Claude Tools Integration](https://quarkus.io/assets/images/posts/mcp/claude-tools.png)

You can also run the server directly without using java - then it would be something like `java -jar <FULL PATH>/weather-1.0.0-SNAPSHOT-runner.jar`. We use JBang here because simpler if you want to share with someone who does not want to build the MCP server locally.

[](https://quarkus.io/blog/mcp-server/#development-tools)Development Tools
--------------------------------------------------------------------------

### [](https://quarkus.io/blog/mcp-server/#mcp-inspector)MCP Inspector

For development and testing, you can use the MCP Inspector tool:

`npx @modelcontextprotocol/inspector`

This starts a local web server where you can test your MCP server:

![Image 3: MCP Inspector Interface](https://quarkus.io/assets/images/posts/mcp/mcp-inspector.png)

### [](https://quarkus.io/blog/mcp-server/#integration-with-langchain4j)Integration with LangChain4j

To use our weather server with LangChain4j, add this configuration:

```
quarkus.langchain4j.mcp.weather.transport-type=stdio
quarkus.langchain4j.mcp.weather.command=jbang,--quiet,org.acme:weather:1.0.0-SNAPSHOT:runner
```

[](https://quarkus.io/blog/mcp-server/#other-clientsmcp-hosts)Other Clients/MCP Hosts
-------------------------------------------------------------------------------------

The Model Context Protocol has a page listing [known clients](https://modelcontextprotocol.io/clients).

While I have not tested all the various clients and MCP hosts, the similar approach of using `jbang --quiet <GAV>` should work for most if not all of them.

[](https://quarkus.io/blog/mcp-server/#testing-the-server)Testing the Server
----------------------------------------------------------------------------

You can test the server through Claude or other MCP hosts with queries like:

*   "What is the weather forecast for Solvang?"

*   "What are the weather alerts for New York?"

Here’s what happens behind the scenes:

1.   Your question goes to the LLM along with available tools information

2.   The LLM analyzes the question and determines which tools to use

3.   The client executes the selected tools via the MCP server

4.   Results return to the LLM

5.   The LLM formulates an answer using the tool results

6.   You see the final response!

[](https://quarkus.io/blog/mcp-server/#conclusion)Conclusion
------------------------------------------------------------

We’ve seen how Quarkus makes implementing an MCP server straightforward, requiring minimal boilerplate code compared to other implementations. The combination of Quarkus’s extension system and JBang makes development and deployment quite a joy.
