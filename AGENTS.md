# AGENTS.md

This guide provides instructions for automated agents or developers contributing to this repository.

## Table of Contents
- [Quick Start](#quick-start)
- [Code Quality](#code-quality)
- [Java Best Practices](#java-best-practices)
- [Package Structure](#package-structure)

## Quick Start

### Building and Running
```bash
make build       # Build the project
make dev         # Start in development mode with live reload
make run         # Run the server in production mode
```

### Makefile Commands
Run `make help` to see all available tasks. Common ones include:
- `make docker-build` / `make docker-run` for Docker support
- `make clean` to remove build artifacts

### Formatting and Style Checks
```bash
make format     # Apply formatting
make checkstyle # Run style checks
```

### Testing
```bash
make test             # Run unit tests
make test-watch       # Continuous testing mode
make test-integration # Run integration tests
./mvnw test -Dtest=MemoryServiceTest  # Run specific test class
```

### Documentation Generation
```bash
make update-readme      # Update README with generated tool docs
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
