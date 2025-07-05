# AGENTS.md

This guide provides instructions for automated agents or developers contributing to this repository.

## Development Workflow

### Building from Source

```bash
make build       # Build the project
make dev         # Start in development mode with live reload
make run         # Run the server in production mode
```

### Makefile Commands

Run `make help` to see all available tasks. Common ones include:

- `make docker-build` / `make docker-run` for Docker support
- `make clean` to remove build artifacts

### Documentation Generation

```bash
make update-readme      # Update README with generated tool docs
python scripts/update-docs.py  # Run the script directly
```

## Code Quality

- **Palantir Java Format** enforced via Spotless
- **Enhanced Checkstyle** rules for style verification
- **Comprehensive Javadoc** for public APIs
- **Final parameters** for all methods
- **120 character line limit**

Run quality tasks before committing:

```bash
make format     # Apply formatting
make checkstyle # Run style checks
```

## Testing

```bash
make test             # Run unit tests
make test-watch       # Continuous testing mode
make test-integration # Run integration tests
./mvnw test -Dtest=MemoryServiceTest  # Run specific test class
```

## Project Structure

```text
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

For CLAUDE-specific details see [CLAUDE.md](CLAUDE.md).
