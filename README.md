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

# Or run in production mode
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

**memory_search_nodes** - Search for existing entities and content
```json
{
  "query": "Jason"
}
```
*Use this to find entities by name, type, or content. Search for "person", "preferences", or specific topics.*

**memory_open_nodes** - Get complete details for specific entities
```json
{
  "names": ["Jason", "Technical_Preferences"]
}
```
*Retrieve full entity information including all observations and relationships.*

**memory_read_graph** - View the entire knowledge graph
```json
{}
```
*Get a complete picture of all entities, relations, and observations. Use sparingly for large graphs.*

#### Building Knowledge - Entities & Relationships

Once you understand your existing knowledge, start building:

**memory_create_entities** - Create new knowledge nodes
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

**memory_create_relations** - Link entities together
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

**memory_add_observations** - Add new facts to existing entities
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

**memory_delete_entities** - Remove outdated entities
```json
{
  "entityNames": ["Old_Project", "Deprecated_Tool"]
}
```
*This also removes all relations involving these entities.*

**memory_delete_relations** - Remove specific connections
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

**memory_delete_observations** - Remove outdated facts
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
1. Search for existing entities (`memory_search_nodes`)
2. Create new entities if needed (`memory_create_entities`)
3. Link entities with relationships (`memory_create_relations`)
4. Add details over time (`memory_add_observations`)
5. Clean up outdated information (`memory_delete_*`)

### Time Tools

The server provides MCP tools for time and timezone operations:

- **get_current_time**
  - Description: Get current time in a specific timezone
  - Parameters:
    - `timezone` (string): IANA timezone name (e.g., "America/New_York", "Europe/London")
  - Read-only: **true**

- **convert_time**
  - Description: Convert time between timezones
  - Parameters:
    - `sourceTimezone` (string): Source IANA timezone name
    - `time` (string): Time to convert in 24-hour format (HH:MM)
    - `targetTimezone` (string): Target IANA timezone name
  - Read-only: **true**

### Memory Resources

Access memory data via URI resources:

- **memory://types**
  - Description: Returns types and patterns available in the memory graph.

- **memory://status**
  - Description: Returns memory graph status and health information.

### Memory Prompts

Guidance prompts for memory management:

- **memory_best_practices**
  - Description: Guide Claude on effective memory management patterns

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
