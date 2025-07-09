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

The server provides MCP tools for managing a persistent knowledge graph:

#### Entity Management

- **memory_create_entities**
  - Description: Create multiple new entities in the knowledge graph
  - Parameters:
    - `entities` (array): Array of entities with name, entityType, and observations
  - Read-only: **false**

- **memory_delete_entities**
  - Description: Remove entities and their relations from the knowledge graph
  - Parameters:
    - `entityNames` (array): Array of entity names to delete
  - Read-only: **false**

#### Relationship Management

- **memory_create_relations**
  - Description: Create multiple new relations between entities
  - Parameters:
    - `relations` (array): Array of relations with from, to, and relationType
  - Read-only: **false**

- **memory_delete_relations**
  - Description: Remove specific relations from the knowledge graph
  - Parameters:
    - `relations` (array): Array of relations with from, to, and relationType
  - Read-only: **false**

#### Observation Management

- **memory_add_observations**
  - Description: Add new observations to existing entities
  - Parameters:
    - `observations` (array): Array of objects with entityName and contents
  - Read-only: **false**

- **memory_delete_observations**
  - Description: Remove specific observations from entities
  - Parameters:
    - `deletions` (array): Array of objects with entityName and observations to delete
  - Read-only: **false**

#### Graph Operations

- **memory_read_graph**
  - Description: Read the entire knowledge graph
  - Parameters: None
  - Read-only: **true**

- **memory_search_nodes**
  - Description: Search for nodes in the knowledge graph based on a query
  - Parameters:
    - `query` (string): The search query to match against entity names, types, and observation content
  - Read-only: **true**

- **memory_open_nodes**
  - Description: Retrieve specific nodes by name
  - Parameters:
    - `names` (array): Array of entity names to retrieve
  - Read-only: **true**

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
