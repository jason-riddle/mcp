# MCP Memory Server

A Model Context Protocol (MCP) server implementation with persistent memory graph capabilities, built with Quarkus. This server provides memory tools and resources for managing knowledge graphs with entities, relations, and observations.

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Development](#development)
- [Testing](#testing)
- [License](#license)

## Installation

### Prerequisites

- Java 21 or later
- Maven 3.9+ (or use included `./mvnw` wrapper)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/jason-riddle/mcp.git
cd mcp

# Build the project
make build

# Run in development mode
make dev
```

### Using Make Commands

This project includes a Makefile for common tasks:

```bash
# Show all available commands
make help

# Build and test
make build
make test

# Code quality
make format      # Format code with Spotless
make checkstyle  # Run style checks

# Running
make dev         # Development mode with live reload
make run         # Production mode

# Docker
make docker-build
make docker-run

# Cleanup
make clean
```

### Running with Docker

```bash
# Build and run with Make
make docker-build
make docker-run

# Or use Docker directly
docker build -f src/main/docker/Dockerfile.jvm -t mcp-memory-server .
docker run -p 8080:8080 -v $(pwd)/memory.jsonl:/deployments/memory.jsonl mcp-memory-server
```

## Usage

### Starting the Server

The server starts on port 8080 by default. Memory data is persisted to `memory.jsonl` in the working directory.

```bash
# Development mode with live reload
make dev

# Production mode
make run
```

### Memory Tools

The server provides MCP tools for memory graph operations:

- `memory.create_entities` - Create new entities in the knowledge graph
- `memory.create_relations` - Create relationships between entities
- `memory.add_observations` - Add facts/observations to existing entities
- `memory.search_nodes` - Search entities and observations
- `memory.read_graph` - Read the complete memory graph
- `memory.open_nodes` - Retrieve specific entities by name
- `memory.delete_entities` - Remove entities and their relations
- `memory.delete_relations` - Remove specific relationships
- `memory.delete_observations` - Remove observations from entities

### Memory Resources

Access memory data via URI resources:

- `memory://graph` - Complete formatted memory graph
- `memory://types` - Entity and relation type statistics
- `memory://status` - Memory graph health and status information

### Configuration

Configure via `application.properties`:

```properties
# Memory file path (default: memory.jsonl)
memory.file.path=memory.jsonl

# Server port (default: 8080)
quarkus.http.port=8080
```

## Development

### Project Structure

```
src/main/java/com/jasonriddle/mcp/
├── McpMemoryPrompts.java      # MCP prompts providing memory guidance
├── McpMemoryResources.java    # MCP resources (memory://) for graph access
├── McpMemoryTools.java        # MCP tools for graph operations
└── memory/                    # Memory graph implementation
    ├── Entity.java            # Entity record representing graph nodes
    ├── MemoryGraph.java       # Complete graph structure record
    ├── MemoryRecord.java      # Base interface for JSONL records
    ├── MemoryService.java     # Core service for graph persistence and operations
    └── Relation.java          # Relation record representing graph edges
```

### Code Quality

This project follows strict code quality standards:

- **Palantir Java Format** for code formatting
- **Enhanced Checkstyle** rules for style verification
- **Comprehensive Javadoc** documentation for all public APIs
- **Final parameters** required for all methods
- **120 character line limit**

Run code formatting:
```bash
make format
```

Run style checks:
```bash
make checkstyle
```

### Development Mode

Quarkus development mode provides live reload:

```bash
make dev
```

Access the development UI at http://localhost:8080/q/dev/

## Testing

### Running Tests

```bash
# Run all tests
make test

# Run tests in watch mode (continuous testing)
make test-watch

# Run integration tests
make test-integration

# Run specific test class (using Maven directly)
./mvnw test -Dtest=MemoryServiceTest
```

### Test Structure

```
src/test/java/com/jasonriddle/mcp/
├── McpMemoryPromptsTest.java         # Tests for memory prompts
├── McpMemoryResourcesTest.java       # Tests for memory resources
├── McpMemoryToolsTest.java           # Tests for memory tools
├── McpServerSseIntegrationTest.java  # Integration tests for SSE server
└── memory/                           # Memory implementation tests
    └── MemoryServiceTest.java        # Tests for memory service
```

### Integration Testing

The project includes integration tests that verify:

- Memory persistence and retrieval
- MCP tool functionality
- Resource endpoint responses
- Server-Sent Events (SSE) communication

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
