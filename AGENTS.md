# AGENTS.md

This guide provides instructions for automated agents contributing to this repository.

## Table of Contents
- [Quick Start](#quick-start)
- [Code Quality](#code-quality)
- [Java Best Practices](#java-best-practices)
- [Package Structure](#package-structure)
- [Deployment](#deployment)

## Quick Start

### Building and Running

```bash
make build  # Build the project
make dev    # Start in development mode with live reload
make run    # Run the server in production mode
```

### Makefile Commands

This project includes a comprehensive Makefile for all development tasks:

#### Core Commands

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

#### Docker Commands

```bash
# Build and run with Make
make docker-build
make docker-run
```

### Formatting and Style Checks

```bash
make format      # Apply formatting
make checkstyle  # Run style checks
```

### Testing

```bash
make test                             # Run unit tests

make test-integration                 # Run integration tests
# or run directly
# ./mvnw verify -DskipITs=false

./mvnw test -Dtest=MemoryServiceTest  # Run specific test class
```

## Code Quality

### Code Style: Enhanced Palantir Configuration

This project follows **Palantir Java Format** with enhanced Checkstyle rules based on Palantir Baseline.

#### Formatting Tools

- **Spotless** with Palantir Java Format for automatic code formatting
- **Checkstyle** with Palantir-inspired configuration for style verification

#### Key Style Rules

- **Final parameters:** All method and constructor parameters must be `final`
- **Method length:** Maximum 150 lines per method
- **Parameter count:** Maximum 8 parameters per method
- **Magic numbers:** Only `-1, 0, 1, 2, 8, 10, 16, 100, 1000` allowed as literals
- **Abbreviations:** Allow `XML, HTTP, JSON, API, URL, URI, UUID, DTO, MCP, SSE`
- **No trailing whitespace:** Enforced with regex checks
- **LF line endings:** Unix-style line endings required
- **No star imports:** All imports must be explicit
- **Static imports:** Restricted to specific utility classes (Preconditions, Mockito, etc.)
- **Interface types:** Must use `List`, `Set`, `Map` instead of concrete implementations in APIs
- **No System.out/err:** Must use proper logging instead of console output
- **No object instantiation in method calls:** Extract object creation to separate variables for debugging
- **Forbidden imports:** Bans dangerous packages (sun.*, junit.framework.*, etc.)

### Javadoc Standards

This project follows strict javadoc standards for all Java files.

#### Class-level Documentation

- All classes must have javadoc comments describing their purpose
- Use plain text without HTML tags (no `<p>`, `<br>`, etc.)
- End descriptions with periods
- Format: `/** Brief description of the class. */`

#### Method Documentation

- All public methods must have javadoc comments
- Include @param tags for all parameters with lowercase descriptions
- Include @return tags for non-void methods with lowercase descriptions
- End all descriptions with periods
- Format:
  ```java
  /**
   * Brief description of what the method does.
   *
   * @param paramName parameter description
   * @return description of return value
   */
  ```

#### Record Documentation

- All record classes must document their components with @param tags
- Format:
  ```java
  /**
   * Brief description of the record.
   *
   * @param field1 description of field1
   * @param field2 description of field2
   */
  ```

#### Package Documentation

- All packages must have package-info.java files with concise descriptions
- Format: `/** Brief package description. */`

#### Test Documentation

- Test classes should have class-level javadoc explaining their purpose
- Individual test methods should have brief descriptions
- Use plain text without HTML formatting

## Java Best Practices

#### Single Responsibility Principle (SRP)

- Each class should have only one reason to change
- Ask: "Can I describe this class's purpose in one sentence without using 'and'?"
- Break complex classes into smaller, focused components
- Methods should do one thing well

#### Method Design

- Target 5-15 lines per method (examine methods over 20 lines)
- Split methods when they need internal comments to explain sections
- Extract methods when logic can be reused or has clear responsibilities
- Use descriptive method names that explain what they do

#### Naming Conventions

- Classes: PascalCase nouns (`UserService`, `PaymentProcessor`)
- Methods: lowerCamelCase verbs (`calculateTotal()`, `validateInput()`)
- Variables: lowerCamelCase descriptive nouns (`userName`, `connectionPool`)
- Constants: UPPER_SNAKE_CASE (`MAX_RETRY_ATTEMPTS`, `API_BASE_URL`)
- Avoid generic names like `Manager`, `Helper`, single letters (except loop counters)

#### Cohesion and Coupling

- High Cohesion: Keep related methods and data together in the same class
- Loose Coupling: Depend on abstractions (interfaces) rather than concrete classes
- Law of Demeter: Objects should only talk to immediate dependencies
- Use dependency injection to avoid tight coupling

#### Code Organization

- Classes should generally be under 200-300 lines
- 5-20 methods per class is typical
- Organize class members: constants, static variables, instance variables, constructors, public methods, private methods
- Package by feature, not by technical layers

#### Red Flags to Watch For

- Methods with more than 3-4 parameters
- Deeply nested control structures (>3 levels)
- Classes that are difficult to name clearly
- Methods that modify global state unexpectedly
- Duplicate code across multiple classes
- Classes that change frequently for different reasons

## Package Structure

This project follows a feature-based package organization.

### Main Package Structure

```text
com.jasonriddle.mcp/
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

### Test Package Structure

```text
com.jasonriddle.mcp/
├── McpMemoryPromptsTest.java                        # Tests for memory prompts
├── McpMemoryResourcesTest.java                      # Tests for memory resources
├── McpMemoryToolsTest.java                          # Tests for memory tools
├── McpServerSseIntegrationTest.java                 # Integration tests for SSE server
├── security/                                        # Security-related tests
│   └── ApiKeyAuthenticationDisabledIntegrationTest.java  # Tests with security disabled
└── memory/                                          # Memory implementation tests
    └── MemoryServiceTest.java                       # Tests for memory service
```

**Note**: Authentication integration tests were removed because authentication is not functional for MCP SSE endpoints.

### Package Documentation

Each package contains a `package-info.java` file with purpose and responsibilities:
- **Root package** (`com.jasonriddle.mcp`): Model Context Protocol server implementation with memory graph capabilities
- **Memory package** (`com.jasonriddle.mcp.memory`): Memory graph implementation for the MCP server
- **Test packages**: Corresponding test suites for implementation verification

### Design Principles

- **Feature-based organization**: Packages organized around functional areas rather than technical layers
- **Minimal dependencies**: Each package has clear, minimal dependencies on other packages
- **Clear separation**: MCP-specific classes in root, domain logic in memory package
- **Consistent naming**: Package names reflect their primary responsibility
- **Single responsibility**: Each package has one clear purpose and set of related classes

### Configuration

Configure via `application.properties`:

```properties
# Memory file path (default: memory.jsonl)
memory.file.path=memory.jsonl

# Server port (default: 8080)
quarkus.http.port=8080

# Security Configuration (optional)
# Enable API key authentication
mcp.security.enabled=false
# API key value (use environment variable in production)
mcp.security.api-key=${MCP_API_KEY:}
```

#### Security Setup

**⚠️ IMPORTANT: Authentication is currently NOT FUNCTIONAL**

The MCP server configuration includes security settings, but authentication is not implemented for MCP SSE endpoints due to Quarkus MCP extension limitations.

**Current Status**:
- Configuration exists but does not secure endpoints
- All requests succeed regardless of API key validity

**Configuration (Non-functional)**:
```properties
# Security Configuration (NON-FUNCTIONAL)
mcp.security.enabled=false  # Must remain false
mcp.security.api-key=${MCP_API_KEY:}
```

**Production Recommendations**:
- Deploy behind a reverse proxy with authentication (nginx, Apache)
- Use network-level security (VPN, firewall rules)
- Consider container-level authentication if required

### Container Image Tagging Strategy

The project implements a comprehensive Docker image tagging strategy for both local development and CI/CD deployment:

#### Local Development Tags

Local builds use Quarkus container image configuration in `application.properties`:

```properties
# Container Image Configuration
quarkus.container-image.build=true
quarkus.container-image.registry=us-central1-docker.pkg.dev
quarkus.container-image.group=jasons-mcp-server-20250705/mcp-servers
quarkus.container-image.name=jasons-mcp-server
quarkus.container-image.tag=latest
quarkus.container-image.additional-tags=${quarkus.application.version}
```

**Generated Tags:**
- `latest` - Always points to most recent local build
- `{version}` - Maven project version (e.g., `0.0.1-SNAPSHOT`)
- Custom additional tags as specified

#### Tag Generation Process

The build uses a two-step process:

1. **Generate Tag:** Extract Maven version and create timestamp
2. **Build & Push:** Use unique tag for container image and deployment

```bash
# Extract version and create unique tag
MAVEN_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
TIMESTAMP=$(date -u +%Y-%m-%dT%H-%M-%SZ)
UNIQUE_TAG="${MAVEN_VERSION}-build-${BUILD_ID}-timestamp-${TIMESTAMP}"

# Example
# UNIQUE_TAG="0.0.1-SNAPSHOT-build-971a5c67-3956-4b22-ba24-5a0ec426cbeb-timestamp-2025-07-05T16-40-34Z"

# Build with unique tag
mvn clean package -DskipTests=true \
  -Dquarkus.container-image.tag="${UNIQUE_TAG}"
```

### Build Optimization

#### Caching Strategy

- **Maven Dependencies**: Cached in container layers
- **Docker Layers**: Reused across builds
- **Quarkus Build Cache**: Optimizes compilation

### Troubleshooting

#### Quarkus Container Image Issues

##### Invalid Registry Configuration Error

**Error**: `The supplied container-image registry 'us-central1-docker.pkg.dev/project/repo' is invalid`

**Root Cause**: Quarkus container-image extension expects registry hostname and group to be separated

**Solution**: Split the configuration properly in `application.properties`:

```properties
# ❌ Incorrect - includes full path in registry
quarkus.container-image.registry=us-central1-docker.pkg.dev/jasons-mcp-server-20250705/mcp-servers
quarkus.container-image.group=jasons-mcp-server

# ✅ Correct - separate hostname from project/repository path
quarkus.container-image.registry=us-central1-docker.pkg.dev
quarkus.container-image.group=jasons-mcp-server-20250705/mcp-servers
quarkus.container-image.name=jasons-mcp-server
quarkus.container-image.tag=latest
```

#### Debug Commands

```bash
# Verify Docker authentication
docker system info | grep -A5 "Registry Mirrors"

# Test container image build locally
./mvnw clean package -Dquarkus.container-image.build=true

# Check running containers
docker images | grep jasons-mcp-server
```

#### Quarkus Development Issues

##### Integration Test Connection Failures

**Symptoms**: Tests show `HttpClosedException: Connection was closed` during build

**Explanation**: These are normal integration test connection attempts. They don't indicate build failures if the main build succeeds.

**Resolution**: The errors are expected during testing and don't affect deployment. Monitor for actual build failure messages in Maven output.

##### JIB Multi-platform Warnings

**Symptoms**: Warnings about base image digests and multi-platform builds

**Solution**: These are suppressed in `application.properties`:

```properties
# Suppress JIB processor warning messages
quarkus.log.category."io.quarkus.container.image.jib.deployment.JibProcessor".level=ERROR
```

#### Authentication Test Failures

**Symptoms**: `ApiKeyAuthenticationIntegrationTest` failures with authentication errors expected but not received

**Root Cause**: MCP STDIO transport operates via standard input/output and doesn't use HTTP endpoints, so traditional HTTP authentication mechanisms don't apply:
- No JAX-RS `@Provider` filters for STDIO
- No HTTP authentication for STDIO transport
- Authentication must be handled at the process level

**Resolution**: Authentication integration tests have been removed. The `ApiKeyAuthenticationDisabledIntegrationTest` verifies functionality without authentication.

#### Debugging Commands

```bash
# Run specific test categories
./mvnw test                                                     # Unit tests only
./mvnw verify -DskipITs=false                                   # Include integration tests
./mvnw test -Dtest=ApiKeyAuthenticationDisabledIntegrationTest  # Security tests
```

## Deployment

### Heroku Deployment

The project is configured for deployment to Heroku as an MCP STDIO server compatible with Heroku's Managed Inference and Agents platform.

#### Configuration Files

- **`Procfile`** - Defines the `mcp-memory` process type required for MCP server registration
- **`system.properties`** - Specifies Java 17 runtime for Heroku
- **`app.json`** - Enables one-click deployment with Managed Inference add-on
- **`application-heroku.properties`** - Heroku-specific Quarkus configuration

#### Key Configuration Approach

The deployment uses a targeted approach that preserves local container build capability while disabling it only for Heroku:

- **Local Development**: Container image building enabled via `application.properties`
- **Heroku Deployment**: Container image building disabled via `MAVEN_CUSTOM_OPTS` config variable
- **No Broad Overrides**: Avoids global Maven configuration that would affect local builds

#### Deployment Commands

```bash
# Initial setup
heroku create your-mcp-server-name
heroku config:set MAVEN_CUSTOM_OPTS="-DskipTests -Dquarkus.container-image.build=false"

# Deploy
git push heroku main

# Add MCP functionality
heroku addons:create heroku-inference:claude-3-5-haiku
```

#### Heroku MCP Integration

Once deployed with the Managed Inference add-on:
- MCP tools are automatically available to Heroku's inference models
- External clients can access via MCP Toolkit URL and Token
- Server automatically scales to zero when not in use

For detailed deployment instructions, see [docs/HEROKU_DEPLOYMENT.md](docs/HEROKU_DEPLOYMENT.md).
