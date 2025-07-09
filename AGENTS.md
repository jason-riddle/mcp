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
./mvnw test -Dtest=TimeServiceTest   # Run time service tests
./mvnw test -Dtest=WeatherServiceTest # Run weather service tests
```

## Code Quality

### Code Style: Enhanced Palantir Configuration

This project follows **Palantir Java Format** with enhanced Checkstyle rules based on Palantir Baseline.

#### Formatting Tools

- **Spotless** with Palantir Java Format for automatic code formatting
- **Checkstyle** with Palantir-inspired configuration for style verification

#### Key Style Rules

**Core Structure:**
- **Final parameters:** All method and constructor parameters must be `final`
- **Method length:** Maximum 150 lines per method
- **Parameter count:** Maximum 8 parameters per method
- **Line length:** Maximum 120 characters (ignoring imports, packages, URLs)
- **Tab width:** 4 spaces
- **Line endings:** Unix-style (LF) required
- **File encoding:** UTF-8 with no BOM

**Naming Conventions:**
- **Magic numbers:** Only `-1, 0, 1, 2, 8, 10, 16, 100, 1000` allowed as literals
- **Abbreviations:** Allow `XML, HTTP, JSON, API, URL, URI, UUID, DTO, MCP, SSE, JUnit`
- **Package names:** Must be all lowercase with dots separating words
- **Classes:** PascalCase nouns
- **Methods:** lowerCamelCase verbs
- **Variables:** lowerCamelCase descriptive nouns
- **Constants:** UPPER_SNAKE_CASE

**Import Management:**
- **No star imports:** All imports must be explicit
- **Static imports:** Restricted to specific utility classes (Preconditions, Mockito, etc.)
- **Forbidden imports:** Bans dangerous packages (sun.*, junit.framework.*, legacy Guava)
- **Unused imports:** Automatically removed

**Type Safety:**
- **Interface types:** Must use `List`, `Set`, `Map` instead of concrete implementations in APIs
- **No primitive wrapper instantiation:** Use autoboxing instead
- **No Collections.* imports:** Use explicit static imports for readability

**Code Quality Patterns:**
- **No System.out/err:** Must use proper logging instead of console output
- **No printStackTrace():** Use proper logging framework
- **No object instantiation in method calls:** Extract object creation to separate variables for debugging
- **No excessive method chaining:** Maximum 3 chained method calls
- **No logical OR in method calls:** Extract to boolean variables
- **No nested Map.of calls:** Extract inner maps to variables

**Modern Java Restrictions:**
- **No streams:** Use simple loops instead of `stream()`, `map()`, `filter()`, `collect()`
- **No Collectors:** Use simple loops for better readability and performance
- **No Optional chaining:** Use simple if statements instead of `flatMap()`, complex `orElse()` chains
- **No CompletableFuture chaining:** Use simple async patterns instead of `thenApply()`, `thenCompose()`
- **No ternary operators:** Use if-else statements for better readability
- **No sealed classes:** Use regular inheritance
- **No pattern matching with guards:** Use simple switch or if-else
- **No module system:** Use classpath-based projects

**Code Complexity:**
- **Cyclomatic complexity:** Maximum 8 per method
- **NPath complexity:** Maximum 100 per method
- **No nested blocks:** Avoid unnecessary code blocks
- **No complex if statements:** Extract multiple && conditions to boolean variables
- **No tautological expressions:** Remove `x || !x` patterns
- **No contradictory expressions:** Remove `x && !x` patterns

**Variable Usage:**
- **No var with diamond operator:** Use explicit type or clear initialization
- **No var with complex expressions:** Extract to intermediate variables
- **No hidden fields:** Avoid shadowing instance variables (except constructors/setters)
- **No multiple variable declarations:** One variable per line

**Testing Exceptions:**
- Test files have relaxed rules for static imports, javadoc, magic numbers, string literals
- Console output allowed in tests for debugging
- Some complexity rules relaxed for test setup

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
src/main/java/com.jasonriddle.mcp/
├── McpMemoryPrompts.java      # MCP prompts providing memory guidance
├── McpMemoryResources.java    # MCP resources (memory://) for graph access
├── McpMemoryTools.java        # MCP tools for graph operations
├── McpTimeTools.java          # MCP tools for time and timezone operations
├── McpWeatherTools.java       # MCP tools for weather operations
├── package-info.java          # Package documentation
├── memory/                    # Memory graph implementation
│   ├── Entity.java            # Entity record representing graph nodes
│   ├── MemoryGraph.java       # Complete graph structure record
│   ├── MemoryRecord.java      # Base interface for JSONL records
│   ├── MemoryService.java     # Core service for graph persistence and operations
│   ├── Relation.java          # Relation record representing graph edges
│   └── package-info.java      # Package documentation
├── time/                      # Time and timezone services
│   ├── TimeConversionResult.java  # Result record for time conversions
│   ├── TimeService.java       # Core service for time operations
│   └── package-info.java      # Package documentation
└── weather/                   # Weather services and data structures
    ├── WeatherClient.java     # OpenWeatherMap REST client interface
    ├── WeatherData.java       # Current weather data record
    ├── WeatherForecast.java   # Weather forecast data record
    ├── WeatherService.java    # Core service for weather operations
    └── package-info.java      # Package documentation
```

### Test Package Structure

```text
src/test/java/com.jasonriddle.mcp/
├── McpMemoryPromptsTest.java                       # Tests for memory prompts
├── McpMemoryResourcesTest.java                     # Tests for memory resources
├── McpMemoryToolsTest.java                         # Tests for memory tools
├── McpTimeToolsTest.java                           # Tests for time tools
├── McpWeatherToolsTest.java                        # Tests for weather tools
├── package-info.java                               # Package documentation
├── config/                                         # Configuration tests
│   ├── ConfigurationTestConstants.java            # Test constants for configuration
│   ├── ConfigurationValidationTest.java           # Tests for configuration validation
│   ├── HerokuConfigurationValidationTest.java     # Tests for Heroku configuration
│   ├── HerokuTestProfile.java                     # Heroku test profile
│   ├── ProdConfigurationValidationTest.java       # Tests for production configuration
│   ├── ProdTestProfile.java                       # Production test profile
│   └── package-info.java                          # Package documentation
├── memory/                                         # Memory implementation tests
│   ├── MemoryGraphPermutationTest.java            # JQwik permutation tests for memory graph
│   ├── MemoryServiceTest.java                     # Tests for memory service
│   └── package-info.java                          # Package documentation
├── security/                                       # Security tests (disabled)
│   └── ApiKeyAuthenticationDisabledIntegrationTest.java.disabled  # Disabled auth tests
├── server/                                         # Server integration tests
│   ├── McpIntegrationTestBase.java                 # Base class for integration tests
│   ├── McpServerSseIntegrationTest.java            # Integration tests for SSE server
│   ├── McpServerStdioIntegrationTest.java          # Integration tests for STDIO server
│   └── package-info.java                          # Package documentation
├── time/                                           # Time service tests
│   ├── TimeServiceTest.java                       # Tests for time service
│   └── package-info.java                          # Package documentation
└── weather/                                        # Weather service tests
    ├── WeatherServiceTest.java                    # Tests for weather service
    └── package-info.java                          # Package documentation
```

**Note**: Authentication integration tests were removed because authentication is not functional for MCP STDIO endpoints.

### Package Documentation

Each package contains a `package-info.java` file with purpose and responsibilities:
- **Root package** (`com.jasonriddle.mcp`): Model Context Protocol server implementation with memory graph, time, and weather capabilities
- **Memory package** (`com.jasonriddle.mcp.memory`): Memory graph implementation for the MCP server
- **Time package** (`com.jasonriddle.mcp.time`): Time and timezone conversion services for the MCP server
- **Weather package** (`com.jasonriddle.mcp.weather`): Weather services and data structures for the MCP server
- **Test packages**: Corresponding test suites for implementation verification
  - **Config test package** (`com.jasonriddle.mcp.config`): Configuration validation tests for default, Heroku, and production profiles
  - **Memory test package** (`com.jasonriddle.mcp.memory`): Memory service tests including JQwik permutation testing
  - **Security test package** (`com.jasonriddle.mcp.security`): Security tests (currently disabled for STDIO endpoints)
  - **Server test package** (`com.jasonriddle.mcp.server`): Integration tests for MCP server implementations (SSE and STDIO)
  - **Time test package** (`com.jasonriddle.mcp.time`): Time service tests
  - **Weather test package** (`com.jasonriddle.mcp.weather`): Weather service tests

### Design Principles

- **Feature-based organization**: Packages organized around functional areas rather than technical layers
- **Minimal dependencies**: Each package has clear, minimal dependencies on other packages
- **Clear separation**: MCP-specific classes in root, domain logic in memory and time packages
- **Consistent naming**: Package names reflect their primary responsibility
- **Single responsibility**: Each package has one clear purpose and set of related classes

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

## GitHub Actions Integration

This project uses GitHub Actions for automated testing and quality assurance. The key workflow uses Claude Code actions to perform intelligent permutation testing analysis.

### Workflow Directory Structure

```text
.github/
└── workflows/
    ├── ci.yml                        # Standard CI pipeline
    └── claude-permutation-testing.yml  # Claude-powered permutation testing
```

#### Workflow Files

- **`ci.yml`** - Standard CI pipeline with format checks, style checks, unit tests, integration tests, and build
- **`claude-permutation-testing.yml`** - Claude Code action for intelligent permutation testing analysis

#### CI Workflow (`ci.yml`)

The standard CI pipeline includes:
- **Format Check**: Spotless formatting validation
- **Checkstyle Check**: Code style verification
- **Unit Tests**: Core functionality tests
- **Integration Tests**: End-to-end testing
- **Build**: Application packaging

#### Claude Permutation Testing (`claude-permutation-testing.yml`)

Automated permutation testing with Claude analysis:
- **Security Check**: Restricts execution to authorized users and branches
- **Smart Test Selection**: Analyzes code changes to select relevant tests
- **Execution**: Runs targeted JQwik permutation tests
- **Reporting**: Provides analysis and recommendations

### Claude Code Actions Comparison

There are two Claude Code actions available, each designed for different use cases:

#### `anthropics/claude-code-action@beta`
- **Purpose**: Interactive comment-based workflows
- **Supported Events**: `issue_comment`, `pull_request_review_comment`, `issues`, `pull_request`, `pull_request_review`
- **Use Case**: Manual triggering via comments (e.g., "@claude fix this") or automated triggers on PR/issue events
- **Features**: Trigger phrases, GitHub integration, sticky comments, assignee/label triggers
- **Limitations**: Requires GitHub app installation, limited to GitHub-specific workflows

#### `anthropics/claude-code-base-action@beta`
- **Purpose**: Flexible automation for any GitHub event
- **Supported Events**: All GitHub events (`push`, `pull_request`, `issues`, `schedule`, etc.)
- **Use Case**: Automated workflows triggered by code changes, CI/CD pipelines
- **Features**: Direct prompts, system prompt customization, MCP support
- **Limitations**: No comment-based interaction features

### Parameter Differences

| Parameter | `claude-code-action` | `claude-code-base-action` | Notes |
|-----------|---------------------|-------------------------|-------|
| `anthropic_api_key` | ✅ | ✅ | Required for both |
| `github_token` | ✅ | ❌ | Auto-provided for comment action |
| `trigger_phrase` | ✅ | ❌ | Only for comment-based triggers |
| `custom_instructions` | ✅ | ❌ | Use `append_system_prompt` instead |
| `direct_prompt` | ✅ | ❌ | Use `prompt` instead |
| `prompt` | ❌ | ✅ | Direct prompt for base action |
| `append_system_prompt` | ❌ | ✅ | Append to system prompt |
| `system_prompt` | ❌ | ✅ | Override system prompt |
| `allowed_tools` | ✅ | ✅ | Same format for both |
| `timeout_minutes` | ✅ | ✅ | Same for both |
| `max_turns` | ✅ | ✅ | Same for both |
| `mcp_config` | ❌ | ✅ | MCP server configuration |
| `claude_env` | ❌ | ✅ | Custom environment variables |

### Example Workflows

The project uses Claude Code actions for various automated workflows:

#### 1. Automated Permutation Testing

The project uses `claude-code-base-action` for automated permutation testing analysis:

```yaml
name: Claude Smart Permutation Testing

on:
  push:
    branches: [main]
    paths:
      - 'src/**/*.java'
      - '!src/**/package-info.java'
      - '!src/**/*Test.java'
  issue_comment:
    types: [created]
  pull_request_review_comment:
    types: [created]

jobs:
  claude-permutation-analysis:
    runs-on: ubuntu-latest
    steps:
      - name: Claude Permutation Testing Analysis
        uses: anthropics/claude-code-base-action@beta
        with:
          anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}
          timeout_minutes: 4
          max_turns: "5"
          append_system_prompt: |
            You are an expert in permutation testing and software quality assurance.
            Analyze code changes and run appropriate JQwik permutation tests.
          prompt: |
            Analyze the changes in this commit and execute appropriate permutation tests.
            Focus on being smart about test selection.
          allowed_tools: |
            Edit,Read,Glob,Grep,LS,TodoWrite,Task
            Bash(./mvnw test -Dtest=MemoryGraphPermutationTest*)
            Bash(./mvnw test -Dtest=*PermutationTest*)
            Bash(git diff*),Bash(git log*),Bash(git show*)
```

#### 2. Automated PR Code Review

Uses `claude-code-action` for interactive code review triggered by PR events:

```yaml
name: Claude Auto Review

on:
  pull_request:
    types: [opened, synchronize]

jobs:
  auto-review:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      pull-requests: read
      id-token: write
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 1

      - name: Automatic PR Review
        uses: anthropics/claude-code-action@beta
        with:
          anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}
          timeout_minutes: "60"
          direct_prompt: |
            Please review this pull request and provide comprehensive feedback.

            Focus on:
            - Code quality and best practices
            - Potential bugs or issues
            - Performance considerations
            - Security implications
            - Test coverage
            - Documentation updates if needed

            Provide constructive feedback with specific suggestions for improvement.
            Use inline comments to highlight specific areas of concern.
```

#### 3. Automated Issue Triage

Uses `claude-code-base-action` for automated issue labeling and triage:

```yaml
name: Claude Issue Triage

on:
  issues:
    types: [opened]

jobs:
  triage-issue:
    runs-on: ubuntu-latest
    timeout-minutes: 10
    permissions:
      contents: read
      issues: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup GitHub MCP Server
        run: |
          mkdir -p /tmp/mcp-config
          cat > /tmp/mcp-config/mcp-servers.json << 'EOF'
          {
            "mcpServers": {
              "github": {
                "command": "docker",
                "args": [
                  "run",
                  "-i",
                  "--rm",
                  "-e",
                  "GITHUB_PERSONAL_ACCESS_TOKEN",
                  "ghcr.io/github/github-mcp-server:sha-7aced2b"
                ],
                "env": {
                  "GITHUB_PERSONAL_ACCESS_TOKEN": "${{ secrets.GITHUB_TOKEN }}"
                }
              }
            }
          }
          EOF

      - name: Run Claude Code for Issue Triage
        uses: anthropics/claude-code-base-action@beta
        with:
          prompt: |
            You're an issue triage assistant. Analyze the issue and apply appropriate labels.

            1. First, get available labels: `gh label list`
            2. Get issue details: use mcp__github__get_issue
            3. Analyze content and apply relevant labels
            4. Do not post comments, only apply labels
          allowed_tools: "Bash(gh label list),mcp__github__get_issue,mcp__github__get_issue_comments,mcp__github__update_issue,mcp__github__search_issues,mcp__github__list_issues"
          mcp_config: /tmp/mcp-config/mcp-servers.json
          timeout_minutes: "5"
          anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}
```

### Migration Guide

When switching from `claude-code-action` to `claude-code-base-action`:

1. **Update Action Reference**:
   ```yaml
   # Old
   uses: anthropics/claude-code-action@beta

   # New
   uses: anthropics/claude-code-base-action@beta
   ```

2. **Update Parameters**:
   ```yaml
   # Old
   custom_instructions: |
     System prompt content
   direct_prompt: |
     Direct prompt content
   github_token: ${{ secrets.GITHUB_TOKEN }}
   trigger_phrase: "@claude-test"

   # New
   append_system_prompt: |
     System prompt content
   prompt: |
     Direct prompt content
   # Remove github_token and trigger_phrase
   ```

3. **Update Event Triggers**:
   ```yaml
   # For push events, use base action
   on:
     push:
       branches: [main]

   # For comment events, either action works
   on:
     issue_comment:
       types: [created]
   ```

### Best Practices

1. **Choose the Right Action**:
   - Use `claude-code-action` for manual, comment-triggered workflows
   - Use `claude-code-base-action` for automated, event-triggered workflows

2. **Security Configuration**:
   - Always use `${{ secrets.ANTHROPIC_API_KEY }}` for API keys
   - Limit `allowed_tools` to minimum required permissions
   - Set appropriate `timeout_minutes` to prevent runaway executions

3. **Tool Restrictions**:
   - Use specific patterns for Bash tools (e.g., `Bash(./mvnw test -Dtest=*Test*)`)
   - Avoid overly broad permissions like `Bash(*)`
   - Test tool permissions in development environment first

4. **Error Handling**:
   - Monitor workflow runs for timeout or permission errors
   - Use `max_turns` to prevent infinite loops
   - Implement proper cleanup in post-actions

### Common Issues

#### "Unsupported event type: push"
- **Cause**: Using `claude-code-action` with push events
- **Solution**: Switch to `claude-code-base-action`

#### Parameter Not Recognized
- **Cause**: Using wrong parameter names between actions
- **Solution**: Use parameter mapping table above

#### Tool Permission Denied
- **Cause**: Tool not listed in `allowed_tools`
- **Solution**: Add specific tool pattern to `allowed_tools`

#### "Unexpected input(s) 'additional_permissions'"
- **Cause**: Using `additional_permissions` parameter with `claude-code-base-action@beta`
- **Solution**: Remove the `additional_permissions` parameter - it's not supported by the base action
- **Context**: This parameter was valid in earlier versions but is no longer supported

## Deployment

### Heroku Deployment

The project is configured for deployment to Heroku as an MCP STDIO server compatible with Heroku's Managed Inference and Agents platform.

#### Prerequisites

- Heroku account with billing enabled (for Managed Inference and Agents add-on)
- Heroku CLI installed
- Git installed
- Java 17+ and Maven for local building

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

#### Deployment Process

```bash
# 1. Login and create app
heroku login
heroku create your-mcp-server-name
heroku git:remote -a your-mcp-server-name

# 2. Configure Maven options for Heroku
heroku config:set MAVEN_CUSTOM_OPTS="-DskipTests -Dquarkus.container-image.build=false"

# 3. Deploy (Automatic deploys from main branch are enabled)
git push origin main

# 4. Add MCP functionality
heroku addons:create heroku-inference:claude-3-5-haiku

# 5. Verify deployment
heroku logs --tail
heroku ps
```

#### Using Your MCP Server

**With Heroku Managed Inference:**
- MCP tools are automatically available to your selected inference model

**With External Clients:**
1. Get MCP Toolkit URL and Token from Heroku Dashboard → Add-on → Tools tab
2. Configure external clients (Claude Desktop, Cursor):
   ```json
   {
     "mcpServers": {
       "heroku-memory": {
         "url": "YOUR_MCP_TOOLKIT_URL",
         "headers": {
           "Authorization": "Bearer YOUR_MCP_TOOLKIT_TOKEN"
         }
       }
     }
   }
   ```

#### Important Considerations

**Memory Persistence:**
- Memory stored at `/app/memory.jsonl` on ephemeral filesystem
- Resets on dyno restart
- For persistence, consider Heroku Postgres or Redis add-ons

**Scaling and Cost:**
- MCP servers auto-scale to 0 when not in use
- Spin up on demand (tool calls limited to 300 seconds)
- Cost: ~$0.0008/second for eco dynos when running

**Environment Variables:**
- `PORT` - Set automatically by Heroku
- `QUARKUS_PROFILE=heroku` - Set via Procfile

#### Troubleshooting

**Container Image Build Error:**
```bash
heroku config:set MAVEN_CUSTOM_OPTS="-DskipTests -Dquarkus.container-image.build=false"
```

**Server Not Starting:**
```bash
heroku logs --tail -a your-app-name
```

**MCP Tools Not Showing:**
- Verify Procfile has `mcp-` prefix
- Ensure Managed Inference add-on is attached
- Check Tools tab in add-on dashboard

#### Local Testing

Test Heroku configuration locally:
```bash
./mvnw clean package -Dquarkus.profile=heroku
PORT=8080 java -Dquarkus.profile=heroku -jar target/quarkus-app/quarkus-run.jar
```
