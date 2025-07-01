# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Code Quality

### Code Style: Enhanced Palantir Configuration

This project follows **Palantir Java Format** with enhanced Checkstyle rules based on Palantir Baseline.

#### Formatting Tools
- **Spotless** with Palantir Java Format for automatic code formatting
- **Checkstyle** with Palantir-inspired configuration for style verification

#### Key Style Rules
- **Line length:** 120 characters maximum
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

#### Style Suppressions
Test files have relaxed rules for:
- Magic numbers and string literals (following Palantir test patterns)
- Visibility modifiers (public fields allowed in tests)
- Static imports (Mockito, AssertJ imports allowed)
- System.out/err usage (debugging in tests)
- Line length for Application files
- Visibility modifiers for DTO/Request/Response classes
- Empty record braces formatting (Spotless vs Checkstyle conflict)

**Palantir Java Format vs Checkstyle Conflicts:**
- Method chaining patterns created by Palantir Java Format may exceed 120-character line limits
- `checkstyle-suppressions.xml` includes LineLength suppressions for affected files:
  - `McpMemoryResources.java` - Method chaining in string building operations
  - `McpMemoryPrompts.java` - Long text content and method concatenation
- These suppressions maintain strict line length rules elsewhere while allowing Palantir's formatting preferences

### Package Structure
- `com.jasonriddle.api` - Main API package containing API endpoints and resources (ApiEndpoints, ApiMemoryEndpoints, McpMemoryTools, McpMemoryResources, McpMemoryPrompts)
  - `memory/` - Memory graph data models and services (Entity, Relation, MemoryGraph, MemoryService)
  - `response/` - JSON response models (records for type-safe serialization)
- All response classes use Java records for immutable, type-safe data transfer
- Jackson automatically handles JSON serialization without manual string building

### Javadoc Standards

This project follows strict javadoc standards for all Java files:

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

### Java Best Practices

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
