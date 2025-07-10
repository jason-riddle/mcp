# AGENTS.md

This guide provides instructions for automated agents contributing to this repository.

## Table of Contents
- [Quick Start](#quick-start)
- [Secrets Management](#secrets-management)
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
make dev                     # Development mode with live reload
make run                     # Production mode

# Docker
make docker-build
make docker-run

# Testing suites
make test-all                # All test suites
make test-integration        # Integration tests
make test-mutation          # PITest mutation testing
make test-mock              # VCR mock tests

# Secrets management
make env-encrypt             # Encrypt .env to .env.enc using SOPS
make env-decrypt             # Decrypt .env.enc to .env using SOPS
```

## Secrets Management

### SOPS Encryption

Environment variables are encrypted using SOPS (Secrets OPerationS) with SSH key encryption. This ensures sensitive data can be safely committed to the repository.

**Configuration Files:**
- `.sops.yaml` - SOPS configuration with SSH public keys
- `.env` - Plaintext environment variables (gitignored)
- `.env.enc` - Encrypted environment variables (committed)

**SSH Keys Used:**
All SSH keys from [jason-riddle.keys](https://github.com/jason-riddle.keys) are supported for encryption:
- 3 ED25519 keys for modern authentication
- 1 RSA key for legacy compatibility

### Usage

**Encrypt secrets before committing:**
```bash
make env-encrypt
```

**Decrypt secrets for local development:**
```bash
make env-decrypt
```

**Development Workflow:**
1. Edit `.env` file with secrets
2. Run `make env-encrypt` to create `.env.enc`
3. Commit `.env.enc` (`.env` is gitignored)
4. Other developers run `make env-decrypt` to get secrets

**Key Features:**
- **Multi-key redundancy**: Any of the 4 SSH private keys can decrypt
- **Binary encryption**: Proper handling of environment file format
- **Automatic validation**: Makefile checks for required files
- **Cross-platform**: Works with both ED25519 and RSA keys

**Security Notes:**
- Never commit `.env` files (automatically gitignored)
- Always encrypt before committing changes
- SSH private keys are required for decryption
- Encrypted files use binary SOPS format for reliability

## Testing

### Test Categories

- **Unit Tests**: `make test` - Core functionality testing
- **Integration Tests**: `make test-integration` - End-to-end MCP server testing
- **Property Tests**: `make test-prop` - JQwik permutation testing for memory graph
- **Mock Tests**: `make test-mock` - VCR testing for weather API without API costs
- **Mutation Tests**: `make test-mutation` - PITest mutation testing for memory package

### Key Testing Commands

```bash
# Weather API testing (VCR)
export WEATHER_API_KEY=your_key
./mvnw test -Dtest=WeatherServiceVCRMockTest -Dvcr.mode=record

# Mutation testing
make test-mutation-memory-only   # Quick MemoryService testing
make test-mutation               # Full memory package
```

## Code Quality

### Formatting and Style

**Tools:**
- **Spotless**: Palantir Java Format for automatic formatting
- **Checkstyle**: Enhanced Palantir-inspired rules

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

### Design Principles

- **Feature-based organization**: Packages by functional areas (memory, time, weather)
- **Minimal dependencies**: Clear separation between packages
- **Single responsibility**: One purpose per package
- **MCP separation**: Protocol classes in root, domain logic in packages

### Key Packages

**Main Source** (`src/main/java/com.jasonriddle.mcp/`):
- `memory/` - Memory graph implementation and persistence
- `time/` - Time and timezone services
- `weather/` - Weather services and OpenWeatherMap integration
- Root - MCP protocol tools and resources

**Test Source** (`src/test/java/com.jasonriddle.mcp/`):
- `config/` - Configuration and profile tests
- `memory/`, `time/`, `weather/` - Feature-specific tests
- `server/` - Integration tests for MCP servers

Generate full structure: `tree . -I "target|docs|devenv*|mvnw*|*.log*" --prune`

## GitHub Actions Integration

**Active CI Pipeline** (`.github/workflows/ci.yml`):
- Format/style checks (Spotless, Checkstyle)
- Unit and integration tests
- Application build

**Claude Code Actions:**
- `claude-code-action@beta` - Interactive, comment-triggered workflows
- `claude-code-base-action@beta` - Automated, event-triggered workflows

See `CLAUDE_CODE_GITHUB_ACTIONS.md` for detailed workflow examples and migration guide.

## Deployment

### Quick Deployment

**Heroku Setup:**
```bash
# Setup and deploy
heroku create your-mcp-server-name
heroku config:set MAVEN_CUSTOM_OPTS="-DskipTests -Dquarkus.container-image.build=false"
git push heroku main
heroku addons:create heroku-inference:claude-3-5-haiku
```

**Local Docker:**
```bash
make docker-build && make docker-run
```

**Validation:**
```bash
make test-e2e                    # Test MCP server functionality
heroku logs --tail -a jasons-mcp-server
```

**Key Configuration Files:**
- `Procfile` - MCP process type definition
- `application-heroku.properties` - Heroku-specific config
- See `REMOTE_MCP_SERVER.md` for client configuration and troubleshooting
