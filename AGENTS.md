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

# Google Cloud (Cloud Run deployment)
make gcloud-push    # Build and push to Artifact Registry
make gcloud-deploy  # Deploy to Cloud Run
make gcloud-proxy   # Start local proxy for MCP clients
make gcloud-logs    # View service logs
make gcloud-status  # Check deployment status

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
./mvnw test -Dtest=MemoryServiceTest  # Run specific test class
```

### Documentation Generation
```bash
make update-readme             # Update README with generated tool docs
python scripts/update-docs.py  # Run the script directly
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
├── McpMemoryPromptsTest.java         # Tests for memory prompts
├── McpMemoryResourcesTest.java       # Tests for memory resources
├── McpMemoryToolsTest.java           # Tests for memory tools
├── McpServerSseIntegrationTest.java  # Integration tests for SSE server
└── memory/                           # Memory implementation tests
    └── MemoryServiceTest.java        # Tests for memory service
```

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

## Deployment

### Overview
The MCP server is deployed to Google Cloud Run with IAM authentication. For local development, see [Quick Start](#quick-start).

**Quick Reference:**
```bash
make gcloud-push && make gcloud-deploy  # Deploy updated version
make gcloud-proxy                       # Start local proxy for MCP clients
make gcloud-status                      # Check deployment status
make gcloud-logs                        # View service logs
```

### Service Configuration
- **Project**: jasons-mcp-server-20250705
- **Service**: jasons-mcp-server
- **Region**: us-central1
- **Service URL**: https://jasons-mcp-server-711952654932.us-central1.run.app
- **MCP Endpoint**: `/v1/mcp/sse` (Server-Sent Events transport)
- **Authentication**: Google Cloud IAM required (no unauthenticated access)
- **Scaling**: Auto-scaling from 0 to 10 instances
- **Resources**: 512Mi memory, 1 CPU, 300s timeout

### Local Access
Use the Cloud Run proxy for secure local MCP client connections:

```bash
# Start proxy
make gcloud-proxy

# Configure MCP client
# URL: http://localhost:3000/v1/mcp/sse
```

### Local Deployment

#### Development Mode
Run the server locally with live reload for development:
```bash
make dev
```
Access the development UI at http://localhost:8080/q/dev/

#### Production Mode
Run the server locally in production mode:
```bash
make build
make run
```

#### Docker
Run the server in a containerized environment:
```bash
make docker-build
make docker-run
```

### Cloud Deployment

The server is designed for cloud deployment with Google Cloud Run, providing scalable, managed hosting with built-in authentication.

#### Google Cloud Run

##### Prerequisites
- Google Cloud account with billing enabled
- `gcloud` CLI installed and authenticated
- Docker installed and running

##### Quick Deployment
Deploy to Google Cloud Run in three simple steps:
```bash
# 1. Build and push Docker image to Artifact Registry
make gcloud-push

# 2. Deploy to Cloud Run with authentication
make gcloud-deploy

# 3. Start local proxy for MCP client connections
make gcloud-proxy
```

#### MCP Client Configuration

##### Using Cloud Run Proxy (Recommended)
For local MCP clients, use the Cloud Run proxy for secure access:

1. **Start the proxy**:
   ```bash
   make gcloud-proxy
   ```

2. **Configure your MCP client**:
   ```json
   {
     "mcpServers": {
       "jasons-mcp": {
         "url": "http://localhost:3000/v1/mcp/sse"
       }
     }
   }
   ```

   For clients that don't support the `url` attribute:
   ```json
   {
     "mcpServers": {
       "jasons-mcp": {
         "command": "npx",
         "args": ["-y", "mcp-remote", "http://localhost:3000/v1/mcp/sse"]
       }
     }
   }
   ```

##### Direct Access (Advanced)
For direct access to the Cloud Run service, users need the Cloud Run Invoker IAM role:
```bash
gcloud run services add-iam-policy-binding jasons-mcp-server \
  --region=us-central1 \
  --member="user:email@example.com" \
  --role="roles/run.invoker"
```

#### Management Commands

##### Monitoring
```bash
# Check service status
make gcloud-status

# View recent logs
make gcloud-logs

# Get detailed service information
gcloud run services describe jasons-mcp-server --region us-central1
```

##### Updates
```bash
# Deploy updated version
make gcloud-push && make gcloud-deploy

# Update with new configuration
gcloud run deploy jasons-mcp-server \
  --image IMAGE_URL \
  --region us-central1 \
  --memory 1Gi \
  --cpu 2
```

### Alternative Deployment Options

#### Self-Hosted
Deploy on your own infrastructure:
```bash
# Pull and run the image
docker pull us-central1-docker.pkg.dev/jasons-mcp-server-20250705/mcp-servers/jasons-mcp-server:latest
docker run -p 8080:8080 -v $(pwd)/data:/app/data IMAGE_ID
```

### Configuration

Configure via `application.properties`:

```properties
# Memory file path (default: memory.jsonl)
memory.file.path=memory.jsonl

# Server port (default: 8080)
quarkus.http.port=8080
```

### Testing

#### Running Tests

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

#### Integration Testing

The project includes integration tests that verify:

- Memory persistence and retrieval
- MCP tool functionality
- Resource endpoint responses
- Server-Sent Events (SSE) communication

### Documentation Generation

```bash
# Update README.md with latest tool and resource documentation
make update-readme

# Or run directly
python scripts/update-docs.py
```

### Troubleshooting

#### Common Issues
1. **Authentication errors**: Ensure `gcloud auth login` is current
2. **Billing not enabled**: Link a billing account to the project
3. **Permission denied**: Verify IAM roles (Cloud Run Admin, Artifact Registry Writer)
4. **Proxy connection fails**: Check port 3000 availability and service status

#### Debug Commands
```bash
# Check authentication and project
gcloud auth list
gcloud config list

# Verify service deployment
gcloud run services list --region us-central1

# Test proxy connection
curl -I http://localhost:3000/v1/mcp/sse
```
