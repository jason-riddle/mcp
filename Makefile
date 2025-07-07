# Modern Makefile best practices
.PHONY: help clean build dev run format checkstyle test test-watch test-integration docker-build docker-run docker-clean
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
	@awk 'BEGIN {FS = ":.*##"; printf "Available targets:\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  %-20s %s\n", $$1, $$2 }' $(MAKEFILE_LIST)

clean: ## Clean build artifacts
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

build: ## Build the project
	@echo "Building project..."
	@$(MVN) clean package -q
	@if [ -f "$(JAR_FILE)" ]; then \
		echo "✓ Build successful: $(JAR_FILE)"; \
	else \
		echo "✗ Build failed"; \
		exit 1; \
	fi

dev: ## Run in development mode with hot reload
	@echo "Starting development mode..."
	@echo "Press Ctrl+C to stop"
	@$(MVN) quarkus:dev -q

run: $(JAR_FILE) ## Run the MCP server
	@echo "Starting MCP Server..."
	@echo "Server will communicate via STDIO"
	@echo "Press Ctrl+C to stop"
	java -jar $(JAR_FILE)

# ============================================================================
# QUALITY TARGETS
# ============================================================================

format: ## Format code using Spotless
	@echo "Code formatting..."
	$(MVN) spotless:check
	$(MVN) spotless:apply
	@echo "✓ Formatted files"

checkstyle: ## Run checkstyle verification
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

test: ## Run unit tests
	@echo "Running unit tests..."
	@rm -f memory.jsonl
	@$(MVN) test --no-transfer-progress
	@echo "✓ Unit tests completed"

test-watch: ## Run tests in watch mode
	@echo "Running tests in watch mode..."
	@echo "Press Ctrl+C to stop"
	@$(MVN) quarkus:test

test-integration: ## Run integration tests
	@echo "Running integration tests..."
	@rm -f memory.jsonl
	@$(MVN) verify -DskipITs=false --no-transfer-progress
	@echo "✓ Integration tests completed"

# ============================================================================
# DOCKER TARGETS
# ============================================================================

docker-build: ## Build Docker image
	@echo "Building Docker image..."
	@$(MVN) package -Dquarkus.container-image.build=true -q
	@echo "✓ Docker image built: $(DOCKER_IMAGE)"

docker-run: $(JAR_FILE) ## Run the application in Docker
	@echo "Running $(PROJECT_NAME) in Docker..."
	@echo "Press Ctrl+C to stop"
	docker run -i --rm $(DOCKER_IMAGE)

docker-clean: ## Remove Docker images for this project
	@echo "Removing Docker images for $(PROJECT_NAME)..."
	@docker images -q $(PROJECT_NAME) | xargs -r docker rmi -f
	@echo "✓ Docker images removed"

# ============================================================================
# UTILITY RULES
# ============================================================================

# Ensure JAR file exists before running
$(JAR_FILE):
	@if [ ! -f "$(JAR_FILE)" ]; then \
		echo "JAR file not found. Building..."; \
		$(MAKE) build; \
	fi
