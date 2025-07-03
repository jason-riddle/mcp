# Modern Makefile best practices
.PHONY: help clean build dev run format checkstyle test test-watch test-integration docker-build docker-run docker-clean update-readme
.DELETE_ON_ERROR:
.ONESHELL:

# Shell configuration
SHELL := /bin/bash
.SHELLFLAGS := -eu -o pipefail -c

# ============================================================================
# PROJECT CONFIGURATION
# ============================================================================
PROJECT_NAME := jasons-mcp-server
JAR_FILE := target/quarkus-app/quarkus-run.jar
MVN := ./mvnw

# ============================================================================
# DOCKER CONFIGURATION
# ============================================================================
DOCKER_IMAGE := $(PROJECT_NAME):latest

# Default goal
.DEFAULT_GOAL := help

# ============================================================================
# CORE TARGETS
# ============================================================================

## Show available targets
help:
	@echo "Available targets:"
	@echo ""
	@echo "Core:"
	@echo "  build     - Build the project"
	@echo "  clean     - Clean build artifacts"
	@echo "  dev       - Run in development mode with hot reload"
	@echo "  run       - Run the MCP server"
	@echo ""
	@echo "Quality:"
	@echo "  format      - Format code using Spotless"
	@echo "  checkstyle  - Run checkstyle verification"
	@echo ""
	@echo "Testing:"
	@echo "  test             - Run unit tests"
	@echo "  test-watch       - Run tests in watch mode"
	@echo "  test-integration - Run integration tests"
	@echo ""
	@echo "Docker:"
	@echo "  docker-build - Build Docker image"
	@echo "  docker-run   - Run in Docker"
	@echo "  docker-clean - Remove Docker images"
	@echo ""
	@echo "Documentation:"
	@echo "  update-readme - Update README.md with generated tool documentation"

## Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	@$(MVN) clean -q
	@echo "Cleaning compiled class files..."
	@find . -name "*.class" -type f -delete
	@echo "Cleaning staging directory..."
	@if [ -d "staging" ]; then \
		find staging -type f ! -name ".gitignore" -delete; \
		echo "✓ Staging directory cleaned (preserved .gitignore)"; \
	else \
		echo "✓ No staging directory to clean"; \
	fi
	@echo "✓ Clean complete"

## Build the project
build:
	@echo "Building project..."
	@$(MVN) clean package -q
	@if [ -f "$(JAR_FILE)" ]; then \
		echo "✓ Build successful: $(JAR_FILE)"; \
	else \
		echo "✗ Build failed"; \
		exit 1; \
	fi

## Run in development mode with hot reload
dev:
	@echo "Starting development mode..."
	@echo "Press Ctrl+C to stop"
	@$(MVN) quarkus:dev -q

## Run the MCP server
run: $(JAR_FILE)
	@echo "Starting MCP Server..."
	@echo "Server will communicate via STDIO"
	@echo "Press Ctrl+C to stop"
	java -jar $(JAR_FILE)

# ============================================================================
# QUALITY TARGETS
# ============================================================================

## Format code using Spotless
format:
	@echo "Code formatting..."
	$(MVN) spotless:apply
	@echo "✓ Formatted files"

## Run checkstyle verification
checkstyle:
	@echo "Running checkstyle verification..."
	@if $(MVN) checkstyle:check -q; then \
		echo "✓ Checkstyle passed"; \
	else \
		echo "✗ Checkstyle violations found"; \
		echo "Run 'mvn checkstyle:checkstyle' for detailed report"; \
		exit 1; \
	fi

# ============================================================================
# TESTING TARGETS
# ============================================================================

## Run unit tests
test:
	@echo "Running unit tests..."
	@$(MVN) test -q
	@echo "✓ Unit tests completed"

## Run tests in watch mode
test-watch:
	@echo "Running tests in watch mode..."
	@echo "Press Ctrl+C to stop"
	@$(MVN) quarkus:test -q

## Run integration tests
test-integration:
	@echo "Running integration tests..."
	@$(MVN) verify -DskipITs=false -q
	@echo "✓ Integration tests completed"

# ============================================================================
# DOCKER TARGETS
# ============================================================================

## Build Docker image
docker-build:
	@echo "Building Docker image..."
	@$(MVN) package -Dquarkus.container-image.build=true -q
	@echo "✓ Docker image built: $(DOCKER_IMAGE)"

## Run the application in Docker
docker-run: $(JAR_FILE)
	@echo "Running $(PROJECT_NAME) in Docker..."
	@echo "Press Ctrl+C to stop"
	docker run -i --rm $(DOCKER_IMAGE)

## Remove Docker images for this project
docker-clean:
	@echo "Removing Docker images for $(PROJECT_NAME)..."
	@docker images -q $(PROJECT_NAME) | xargs -r docker rmi -f
	@echo "✓ Docker images removed"

# ============================================================================
# DOCUMENTATION TARGETS
# ============================================================================

## Update README.md with generated tool documentation
update-readme: $(JAR_FILE)
	@echo "Updating README.md with generated documentation..."
	@python scripts/update-docs.py
	@echo "✓ README.md updated with latest tool and resource documentation"

# ============================================================================
# UTILITY RULES
# ============================================================================

# Ensure JAR file exists before running
$(JAR_FILE):
	@if [ ! -f "$(JAR_FILE)" ]; then \
		echo "JAR file not found. Building..."; \
		$(MAKE) build; \
	fi
