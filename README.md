# Jason's MCP Server

A Model Context Protocol (MCP) server implementation with persistent memory graph capabilities, built with Quarkus. This server provides memory tools and resources for managing knowledge graphs with entities, relations, and observations. It uses STDIO transport for communication, making it suitable for subprocess-based MCP integrations.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Installation & Configuration](#installation--configuration)
- [Usage](#usage)
- [Development](#development)
- [Deployment](#deployment)
- [License](#license)

## Overview

The MCP Server implements the Model Context Protocol to provide persistent memory capabilities and time/timezone utilities for AI agents and applications. It maintains a knowledge graph of entities, relations, and observations that can be queried and modified through standardized MCP tools and resources. Additionally, it provides time and timezone conversion tools for working with different time zones.

## Quick Start

```bash
# Clone
git clone https://github.com/jason-riddle/mcp.git
cd mcp

# Build
make build

# Run in development mode (with live reload and dev UI)
make dev

# Or run in development mode
make run
```

The server communicates via standard input/output (STDIO) with memory persisted to `memory.jsonl`.

Run `make help` to see all available commands.

## Installation & Configuration

### Prerequisites

- Java 17 or later
- Maven 3.9+ (or use included `./mvnw` wrapper)

### Building from Source

```bash
# Clone
git clone https://github.com/jason-riddle/mcp.git
cd mcp

# Build
make build
```

### Configuration

Configure via `application.properties`:

```properties
# Memory file path (default: memory.jsonl)
memory.file.path=memory.jsonl

# STDIO transport is enabled for MCP communication
quarkus.mcp.server.stdio.enabled=true
```

## Usage

### Starting the Server

```bash
# Development mode (with live reload and dev UI)
make dev

# Production mode
make run

# Docker
make docker-build && make docker-run
```

The server runs in STDIO mode, communicating via stdin/stdout for MCP protocol messages.

### Memory Tools

The server provides MCP tools for managing a persistent knowledge graph. This section walks you through the typical workflow from discovery to building and maintaining your knowledge base.

#### Getting Started - Discovery & Exploration

Before creating new entities, explore what already exists in your memory graph:

**memory.search_nodes** - Search for existing entities and content
```json
{
  "query": "Jason"
}
```
*Use this to find entities by name, type, or content. Search for "person", "preferences", or specific topics.*

**memory.open_nodes** - Get complete details for specific entities
```json
{
  "names": ["Jason", "Technical_Preferences"]
}
```
*Retrieve full entity information including all observations and relationships.*

**memory.read_graph** - View the entire knowledge graph
```json
{}
```
*Get a complete picture of all entities, relations, and observations. Use sparingly for large graphs.*

#### Building Knowledge - Entities & Relationships

Once you understand your existing knowledge, start building:

**memory.create_entities** - Create new knowledge nodes
```json
{
  "entities": [
    {
      "name": "Jason",
      "entityType": "person",
      "observations": ["Software developer", "Prefers dark themes", "Uses vim keybindings"]
    },
    {
      "name": "Technical_Preferences",
      "entityType": "preferences",
      "observations": ["Dark mode enabled", "Vim keybindings preferred", "Monospace fonts"]
    }
  ]
}
```
*Common entity types: `person`, `preferences`, `project`, `system`, `tool`*

**memory.create_relations** - Link entities together
```json
{
  "relations": [
    {
      "from": "Jason",
      "to": "Technical_Preferences",
      "relationType": "has_preferences"
    },
    {
      "from": "Jason",
      "to": "VSCode",
      "relationType": "uses_tool"
    }
  ]
}
```
*Common relation types: `has_preferences`, `works_on`, `uses_tool`, `manages_project`, `works_at`*

#### Maintaining Data - Observations & Updates

Keep your knowledge current by adding details and removing outdated information:

**memory.add_observations** - Add new facts to existing entities
```json
{
  "observations": [
    {
      "entityName": "Jason",
      "contents": ["Currently working on authentication module", "Learning Rust programming"]
    },
    {
      "entityName": "Technical_Preferences",
      "contents": ["Recently switched to Neovim", "Prefers TypeScript over JavaScript"]
    }
  ]
}
```
*Add one fact per observation. Be specific and actionable.*

#### Cleanup & Maintenance

**memory.delete_entities** - Remove outdated entities
```json
{
  "entityNames": ["Old_Project", "Deprecated_Tool"]
}
```
*This also removes all relations involving these entities.*

**memory.delete_relations** - Remove specific connections
```json
{
  "relations": [
    {
      "from": "Jason",
      "to": "Old_Company",
      "relationType": "works_at"
    }
  ]
}
```

**memory.delete_observations** - Remove outdated facts
```json
{
  "deletions": [
    {
      "entityName": "Jason",
      "observations": ["Learning Python", "New to programming"]
    }
  ]
}
```

#### Common Patterns & Best Practices

**Entity Naming Conventions:**
- Use underscores for multi-word names: `Technical_Preferences`, `Project_Alpha`
- Be descriptive: `Personal_Calendar` vs `Work_Calendar`
- Stay consistent with existing patterns

**Relationship Patterns:**
- Use active voice: `Jason has_preferences Technical_Preferences`
- Be specific: `currently_uses` vs `previously_used`
- Include temporal context when relevant

**Typical Workflow:**
1. Search for existing entities (`memory.search_nodes`)
2. Create new entities if needed (`memory.create_entities`)
3. Link entities with relationships (`memory.create_relations`)
4. Add details over time (`memory.add_observations`)
5. Clean up outdated information (`memory.delete_*`)

### Time Tools

The server provides MCP tools for time and timezone operations, useful for scheduling, logging, and coordinating across time zones.

#### Getting Current Time

**time.get_current_time** - Get current time in any timezone with DST information
```json
{
  "timezone": "America/New_York"
}
```

**Response:**
```json
{
  "timezone": "America/New_York",
  "datetime": "2024-03-15T14:30:00-04:00",
  "is_dst": true
}
```

*Common timezones: `UTC`, `America/New_York`, `Europe/London`, `Asia/Tokyo`, `Australia/Sydney`, `America/Los_Angeles`, `Europe/Berlin`, `Asia/Shanghai`*

#### Converting Between Timezones

**time.convert_time** - Convert time between any two timezones
```json
{
  "sourceTimezone": "America/New_York",
  "time": "14:30",
  "targetTimezone": "Europe/London"
}
```

**Response:**
```json
{
  "source": {
    "timezone": "America/New_York",
    "datetime": "2024-03-15T14:30:00-04:00",
    "is_dst": true
  },
  "target": {
    "timezone": "Europe/London",
    "datetime": "2024-03-15T19:30:00+01:00",
    "is_dst": false
  },
  "time_difference": "+5.0h"
}
```

#### Time Difference Formats

- **Whole hours**: `+9.0h` (UTC to Tokyo), `-5.0h` (NY to UTC)
- **Fractional hours**: `+5.75h` (UTC to Nepal), `+9.5h` (UTC to Adelaide)
- **Same timezone**: `+0.0h` (UTC to UTC)

#### Common Use Cases

**Scheduling meetings across timezones:**
```json
{
  "sourceTimezone": "America/Los_Angeles",
  "time": "09:00",
  "targetTimezone": "Europe/Berlin"
}
```

**Logging events in UTC:**
```json
{
  "timezone": "UTC"
}
```

**Checking DST status for scheduling:**
The `is_dst` field indicates whether Daylight Saving Time is active, helping with scheduling decisions during DST transitions.

### Weather Tools

The server provides MCP tools for accessing real-time weather information and forecasts through the OpenWeatherMap API. These tools are useful for planning, scheduling, and contextual information.

#### Getting Current Weather

**weather.current** - Get current weather conditions for any location
```json
{
  "location": "New York"
}
```

**Response:**
```json
{
  "location": "New York",
  "temperature": 72.5,
  "condition": "Clear",
  "description": "clear sky",
  "timestamp": "2024-03-15T14:30:00Z"
}
```

*Supports city names, coordinates (lat,lon), and various location formats. All temperatures in Fahrenheit.*

#### Weather Forecasts

**weather.forecast** - Get multi-day weather forecast
```json
{
  "location": "San Francisco",
  "days": 3
}
```

**Response:**
```json
[
  {
    "date": "2024-03-15",
    "high_temp": 68.0,
    "low_temp": 52.0,
    "condition": "Partly Cloudy",
    "description": "partly cloudy"
  },
  {
    "date": "2024-03-16",
    "high_temp": 65.0,
    "low_temp": 48.0,
    "condition": "Rain",
    "description": "light rain"
  }
]
```

*Forecast supports 1-5 days ahead.*

#### Weather Alerts

**weather.alerts** - Get weather warnings and advisories
```json
{
  "location": "Miami"
}
```

**Example responses:**
- `"Heat Warning: Temperature is 98.0°F"`
- `"Cold Warning: Temperature is 20.0°F"`
- `"Wind Warning: Wind speed is 30.0 mph"`
- `"No weather alerts for this location"`

#### Setup Requirements

Weather tools require an OpenWeatherMap API key:

1. **Get API Key**: Sign up at [OpenWeatherMap](https://openweathermap.org/api) (free tier: 1,000 calls/day)
2. **Set Environment Variable**: `export WEATHER_API_KEY=your_api_key_here`
3. **Configure**: Add to `application.properties`:
```properties
weather.api.key=${WEATHER_API_KEY:}
```

#### Common Use Cases

**Planning outdoor activities:**
```json
{
  "location": "Portland",
  "days": 2
}
```

**Checking travel conditions:**
```json
{
  "location": "40.7128,-74.0060"
}
```

**Getting location-specific alerts:**
```json
{
  "location": "Phoenix"
}
```

### Memory Resources

Access memory data via URI resources for discovery and health monitoring. These complement the memory tools by providing overview information.

#### Discovering Available Types

**memory://types** - Explore entity types, relation types, and usage patterns
```markdown
# Memory Graph Types and Patterns

## Entity Types
- **person:** 1 entities
- **preferences:** 1 entities

## Relation Types
- **has_preferences:** 1 connections

## Common Patterns
### Entity Naming Examples
- **person**: Jason, Alice, Bob
- **preferences**: Technical_Preferences, UI_Preferences

### Relationship Examples
- **has_preferences**: Entity_A has_preferences Entity_B
```

*Use this resource to understand your graph structure before creating new entities or relationships.*

#### Health Monitoring

**memory://status** - Check graph health and integrity
```markdown
# Memory Graph Status

## Overview
- **Total Entities:** 2
- **Total Relations:** 1
- **Total Observations:** 4

## Entity Types
- **person:** 1
- **preferences:** 1

## Relation Types
- **has_preferences:** 1

## Storage Information
- **File Path:** memory.jsonl
- **File Size:** 245 bytes
- **Last Modified:** 2024-03-15T10:30:00Z

## Data Integrity
- **Orphaned Relations:** 0
- **Entities with No Observations:** 0
- **Isolated Entities:** 0

## Health Status
✅ **Status:** Healthy - No data integrity issues detected
```

#### Using Resources in Your Workflow

1. **Before creating entities** - Check `memory://types` to see existing patterns
2. **After major changes** - Use `memory://status` to verify integrity
3. **For troubleshooting** - Check health status for orphaned relations or empty entities
4. **For discovery** - Explore available entity and relationship types

#### Integration with Memory Tools

- **memory://types** helps you choose consistent entity types before using `memory.create_entities`
- **memory://status** reveals issues that can be fixed with `memory.delete_*` tools
- Both resources provide context for effective use of `memory.search_nodes`

### Memory Prompts

Access comprehensive guidance for effective memory management. Unlike tools and resources, prompts provide instructional content to help you work with the memory graph effectively.

#### Getting Memory Management Guidance

**memory.best_practices** - Comprehensive guide for effective memory management

This prompt provides detailed guidance on:

**Entity Design Principles**
- When to create new entities vs. adding observations
- Entity naming conventions (`Technical_Preferences`, `Project_Alpha`)
- Entity type guidelines (`person`, `preferences`, `project`, `system`)

**Relationship Modeling**
- Active voice conventions: `Jason has_preferences Technical_Preferences`
- Relationship type standards: `works_at`, `currently_uses`, `previously_worked_at`
- Common relationship patterns for ownership, work, and usage

**Search and Query Strategies**
- Entity search by name and type
- Observation content searching
- Effective search combinations and performance tips

**Observation Management**
- Atomic observation principles (one fact per observation)
- Fact vs. opinion guidelines
- Observation lifecycle and cleanup

**Memory Hygiene**
- Regular cleanup patterns
- Duplicate detection and consolidation
- Graph validation and integrity checks

**Integration Best Practices**
- Working with existing memory files
- Cross-session consistency
- Advanced patterns for hierarchies and temporal modeling

#### How to Use Memory Prompts

1. **Before starting** - Read the best practices guide to understand proper patterns
2. **When planning** - Reference entity and relationship conventions
3. **During cleanup** - Use hygiene guidelines for maintenance
4. **For troubleshooting** - Apply search strategies and validation techniques

#### Key Guidance Examples

**Entity Creation:**
```
✅ "Jason" (person entity) - will accumulate preferences, relationships, work history
✅ "Technical_Preferences" (preferences entity) - referenced by multiple systems
❌ Creating separate entities for each individual preference
```

**Relationship Patterns:**
```
✅ Jason has_preferences Technical_Preferences
✅ Jason works_at Company_Alpha
✅ Jason currently_uses VSCode
❌ Technical_Preferences preferences_of Jason (passive voice)
```

The prompt serves as your comprehensive reference guide for building and maintaining high-quality memory graphs that remain useful and searchable over time.

## Development

### Available Commands

```bash
# Build and run
make build                  # Build the project
make dev                    # Development mode with live reload
make run                    # Production mode

# Docker
make docker-build           # Build Docker image
make docker-run             # Run Docker container

# Code quality
make format                 # Format code with Spotless
make checkstyle             # Run style checks

# Testing
make test                   # Run unit tests
make test-integration       # Run integration tests
make test-fuzz            # Fuzz TimeService with Jazzer


# Cleanup
make clean                  # Clean build artifacts
```

### Development Workflow

1. **Start development server**: `make dev`
2. **Access development UI**: http://localhost:8080/q/dev/
3. **Make changes**: Code changes trigger automatic reload
4. **Run tests**: `make test`
5. **Format code**: `make format`
6. **Check style**: `make checkstyle`

### Testing

```bash
# Run all tests
make test

# Run integration tests only
make test-integration
make test-fuzz

# Run specific test class
./mvnw test -Dtest=MemoryServiceTest
```

## Deployment

### Local Docker Deployment

```bash
make docker-build
make docker-run
```

### Heroku Deployment

Deploy as an MCP server compatible with Heroku's Managed Inference platform:

```bash
# Create and configure Heroku app
heroku create your-mcp-server-name
heroku config:set MAVEN_CUSTOM_OPTS="-DskipTests -Dquarkus.container-image.build=false"

# Deploy (automatic from main branch)
git push heroku main

# Add MCP functionality
heroku addons:create heroku-inference:claude-3-5-haiku
```

**Important Notes:**
- Memory persists at `/app/memory.jsonl` on ephemeral filesystem
- Resets on dyno restart - consider Heroku Postgres for persistence
- Uses `mcp-memory` process type for MCP server registration
- Configured for STDIO transport with Heroku's MCP Toolkit

### Self-Hosted Deployment

Deploy on your own infrastructure:

```bash
# Pull and run the image
docker pull us-central1-docker.pkg.dev/jasons-mcp-server-20250705/mcp-servers/jasons-mcp-server:latest
docker run -p 8080:8080 -v $(pwd)/data:/app/data IMAGE_ID
```

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
