# AGENTS.md

This guide provides instructions for automated agents contributing to this repository.

## Table of Contents
- [Quick Start](#quick-start)
- [Testing](#testing)
- [Code Quality](#code-quality)
- [Package Structure](#package-structure)
- [Deployment](#deployment)

## Quick Start

### Essential Commands

```bash
# Build and test
make build
make test
make format
make checkstyle

# Development
make dev    # Development mode with live reload
make run    # Production mode

# Docker
make docker-build
make docker-run

# Testing suites
make test-all                # All test suites
make test-integration        # Integration tests
make test-mutation          # PITest mutation testing
make test-mock              # VCR mock tests
```

### Configuration

Configure via `application.properties`:

```properties
# Memory file path (default: memory.jsonl)
memory.file.path=memory.jsonl

# Weather API configuration
weather.api.key=${WEATHER_API_KEY:}

# Server port (default: 8080)
quarkus.http.port=8080
```

## Testing

### Test Categories

**Unit Tests**: `make test`
- Core functionality testing
- Fast execution, no external dependencies

**Integration Tests**: `make test-integration`
- End-to-end MCP server testing (SSE/STDIO)
- Configuration validation tests

**Property Tests**: `make test-prop`
- JQwik property-based testing for memory graph
- Permutation testing with random inputs

**Mock Tests**: `make test-mock`
- VCR testing for weather API without API costs
- Recorded HTTP interactions for deterministic testing

**Mutation Tests**: `make test-mutation`
- PITest mutation testing for memory package
- Evaluates test quality by introducing code mutations

### VCR Weather API Testing

Uses EasyVCR Java for deterministic weather API testing:

```bash
# Set API key and record cassettes (first run)
export WEATHER_API_KEY=your_key
./mvnw test -Dtest=WeatherServiceVCRMockTest -Dvcr.mode=record

# Replay cassettes (no API key needed)
./mvnw test -Dtest=WeatherServiceVCRMockTest -Dvcr.mode=replay
```

**Key Features:**
- Records real API responses to cassettes
- Automatically censors API keys for security
- Supports Record/Replay/Auto/Bypass modes
- Eliminates API costs in CI/CD

### PITest Mutation Testing

Evaluates test quality by introducing code mutations:

```bash
make test-mutation-memory-only    # Quick MemoryService testing
make test-mutation               # Full memory package
make test-mutation-incremental   # Faster repeated runs
```

**Configuration:**
- Targets memory package (file I/O, concurrency, persistence)
- Standalone test suite avoids Quarkus DI issues
- Reports in `target/pit-reports/index.html`
- Target: 70%+ mutation score

## Code Quality

### Formatting and Style

**Tools:**
- **Spotless** with Palantir Java Format for automatic formatting
- **Checkstyle** with enhanced Palantir-inspired rules

**Key Rules:**
- Final parameters required
- Maximum 150 lines per method, 8 parameters
- Maximum 120 characters per line
- No star imports, proper interface types
- No streams/collectors (use simple loops)
- No ternary operators (use if-else)
- No System.out (use logging)

### Javadoc Standards

All classes and public methods require javadoc:

```java
/**
 * Brief description of the class.
 */
public class Example {
    /**
     * Brief description of what the method does.
     *
     * @param paramName parameter description
     * @return description of return value
     */
    public String methodName(final String paramName) { ... }
}
```

### Best Practices

**Single Responsibility:**
- One reason to change per class
- 5-15 lines per method (examine >20 lines)
- Descriptive names without 'Manager'/'Helper'

**Code Organization:**
- Feature-based packages
- Classes under 200-300 lines
- 5-20 methods per class
- Package by feature, not layers

## Package Structure

### Architecture

```text
src/main/java/com.jasonriddle.mcp/
├── McpMemoryTools.java        # MCP memory operations
├── McpTimeTools.java          # Time/timezone tools
├── McpWeatherTools.java       # Weather API tools
├── memory/                    # Memory graph implementation
├── time/                      # Time conversion services
└── weather/                   # Weather services

src/test/java/com.jasonriddle.mcp/
├── memory/                    # Memory tests (unit, property, mutation)
├── time/                      # Time service tests
├── weather/                   # Weather tests (unit, VCR)
├── server/                    # Integration tests (SSE, STDIO)
└── config/                    # Configuration tests
```

### Design Principles

- **Feature-based organization**: Packages by functional areas
- **Minimal dependencies**: Clear separation between packages
- **Single responsibility**: One purpose per package
- **MCP separation**: Protocol classes in root, domain logic in packages

## GitHub Actions Integration

### Active Workflows

**CI Pipeline** (`.github/workflows/ci.yml`):
- Format/style checks (Spotless, Checkstyle)
- Unit and integration tests
- Application build

**Disabled Workflows** (reference only):
- `claude-readme-update-check.yml.disabled` - README maintenance
- `claude-smart-permutation-testing.yml.disabled` - Automated testing analysis

### Claude Code Actions

Two action types available:

**`claude-code-action@beta`**: Interactive, comment-triggered workflows
**`claude-code-base-action@beta`**: Automated, event-triggered workflows

Key differences:
- Comment action: Manual triggers, GitHub integration
- Base action: Push/schedule triggers, MCP support, direct prompts

## Deployment

### Heroku Deployment

**Prerequisites:**
- Heroku account with billing enabled
- Heroku CLI, Git, Java 17+

**Configuration Files:**
- `Procfile` - MCP process type definition
- `system.properties` - Java 17 runtime
- `app.json` - One-click deployment
- `application-heroku.properties` - Heroku-specific config

**Deployment Process:**

```bash
# Setup
heroku create your-mcp-server-name
heroku config:set MAVEN_CUSTOM_OPTS="-DskipTests -Dquarkus.container-image.build=false"

# Deploy
git push origin main

# Add MCP capability
heroku addons:create heroku-inference:claude-3-5-haiku

# Verify
heroku logs --tail
```

**MCP Integration:**
- Automatic tool availability with Heroku Managed Inference
- External client configuration via MCP Toolkit URL/Token
- Auto-scaling to 0 when not in use

**Important Considerations:**
- Memory stored on ephemeral filesystem (resets on restart)
- Tool calls limited to 300 seconds
- Cost: ~$0.0008/second for eco dynos when running

### Troubleshooting

**Common Issues:**

**Container Build Error:**
```bash
heroku config:set MAVEN_CUSTOM_OPTS="-DskipTests -Dquarkus.container-image.build=false"
```

**MCP Tools Not Available:**
- Verify Procfile has `mcp-` prefix
- Ensure Managed Inference add-on attached
- Check Tools tab in add-on dashboard

**Local Testing:**
```bash
./mvnw clean package -Dquarkus.profile=heroku
PORT=8080 java -Dquarkus.profile=heroku -jar target/quarkus-app/quarkus-run.jar
```

**Quarkus Registry Configuration:**
Separate registry hostname from project path:
```properties
quarkus.container-image.registry=us-central1-docker.pkg.dev
quarkus.container-image.group=project-id/repository
```
