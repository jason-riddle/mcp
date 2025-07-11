# Modern Makefile best practices
.PHONY: help clean build dev run format checkstyle test test-watch test-integration test-prop test-mock test-e2e test-all test-mutation test-mutation-incremental test-mutation-memory-only docker-build docker-run docker-clean env-encrypt env-decrypt
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

# CI Profile Configuration
ifeq ($(CI),true)
	MAVEN_PROFILE := -Pci-testing
else
	MAVEN_PROFILE :=
endif

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
	@echo "Cleaning log files..."
	@rm -f *.log *.log.*
	@echo "Cleaning memory files..."
	@rm -f *.jsonl
	@echo "✓ Clean complete"

build: ## Build the project
	@echo "Building project..."
	@$(MVN) clean package --no-transfer-progress
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

format: ## Check formatting and apply fixes only if needed
	@echo "Checking code formatting..."
	@if ! $(MVN) spotless:check -q; then \
		echo "Formatting issues found. Applying fixes..."; \
		$(MVN) spotless:apply; \
		echo "✓ Formatting fixes applied"; \
	else \
		echo "✓ Code is already properly formatted"; \
	fi

format-check: ## Check formatting without applying fixes
	@echo "Checking code formatting..."
	@$(MVN) spotless:check --no-transfer-progress
	@echo "✓ Formatting check passed"

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

test: clean ## Run unit tests
	@echo "Running unit tests..."
	@$(MVN) test -q --no-transfer-progress $(MAVEN_PROFILE)
	@echo "✓ Unit tests completed"

test-watch: ## Run tests in watch mode
	@echo "Running tests in watch mode..."
	@echo "Press Ctrl+C to stop"
	@$(MVN) quarkus:test

test-integration: clean ## Run integration tests
	@echo "Running integration tests..."
	@$(MVN) verify -DskipITs=false -q --no-transfer-progress $(MAVEN_PROFILE)
	@echo "✓ Integration tests completed"

test-prop: clean ## Run property tests only
	@echo "Running property tests..."
	@$(MVN) test -Dtest=*PropertyTest -q --no-transfer-progress $(MAVEN_PROFILE)
	@echo "✓ Property tests completed"

test-mock: clean ## Run mock tests only
	@echo "Running mock tests..."
	@$(MVN) test -Dtest=*MockTest -q --no-transfer-progress $(MAVEN_PROFILE)
	@echo "✓ Mock tests completed"

test-e2e: clean ## Run end-to-end tests against remote MCP server
	@echo "Running end-to-end tests..."
	@echo "Testing remote Heroku MCP endpoint with authentication..."
	@$(MVN) test -Dtest=*HerokuEndToEndTest,*E2ETest -q --no-transfer-progress $(MAVEN_PROFILE)
	@echo "✓ End-to-end tests completed"

test-all: clean ## Run all tests (unit, integration, permutation, mock, e2e)
	@echo "Running all tests..."
	@echo "1/5 Running unit tests..."
	@$(MVN) test -q --no-transfer-progress || echo "Unit tests failed - continuing"
	@echo "2/5 Running integration tests..."
	@$(MVN) verify -DskipITs=false -q --no-transfer-progress || echo "Integration tests failed - continuing"
	@echo "3/5 Running property tests..."
	@$(MVN) test -Dtest=*PropertyTest -q --no-transfer-progress || echo "Property tests failed - continuing"
	@echo "4/5 Running mock tests..."
	@$(MVN) test -Dtest=*MockTest -q --no-transfer-progress || echo "Mock tests failed - continuing"
	@echo "5/5 Running end-to-end tests..."
	@$(MVN) test -Dtest=*EndToEndTest,*E2ETest -q --no-transfer-progress || echo "End-to-end tests failed - continuing"
	@echo "✓ All test suites completed (check output above for any failures)"

# ============================================================================
# MUTATION TESTING
# ============================================================================

test-mutation: clean ## Run PITest mutation testing on memory package
	@echo "Running PITest mutation testing on memory package..."
	@$(MVN) clean test-compile org.pitest:pitest-maven:mutationCoverage -Pmemory-mutation-tests -q --no-transfer-progress $(MAVEN_PROFILE)
	@echo "✓ Mutation testing complete. Reports available in target/pit-reports/"

test-mutation-incremental: clean ## Run incremental PITest mutation testing
	@echo "Running incremental PITest mutation testing..."
	@$(MVN) org.pitest:pitest-maven:mutationCoverage -DwithHistory -Pmemory-mutation-tests -q --no-transfer-progress
	@echo "✓ Incremental mutation testing complete"

test-mutation-memory-only: clean ## Run mutation testing only on MemoryService
	@echo "Running mutation testing on MemoryService only..."
	@$(MVN) org.pitest:pitest-maven:mutationCoverage \
	 -DtargetClasses=com.jasonriddle.mcp.memory.MemoryService \
	 -DtargetTests=com.jasonriddle.mcp.memory.*PITMutationTest \
	 -q --no-transfer-progress
	@echo "✓ MemoryService mutation testing complete"

test-fuzz: clean ## Run Jazzer fuzzing on TimeService
	@echo "Running Jazzer fuzzing on TimeService..."
	@$(MVN) test -Dtest=com.jasonriddle.mcp.time.TimeServiceFuzzTest -P fuzz-time -q --no-transfer-progress
	@echo "✓ TimeService fuzzing complete"

# test-stdio: clean ## Run STDIO integration tests only
# 	@echo "Running STDIO integration tests..."
# 	@$(MVN) verify -Dtest=McpServerStdioIntegrationTest --no-transfer-progress
# 	@echo "✓ STDIO integration tests completed"

# test-sse: clean ## Run SSE integration tests only
# 	@echo "Running SSE integration tests..."
# 	@$(MVN) verify -Dtest=McpServerSseIntegrationTest --no-transfer-progress
# 	@echo "✓ SSE integration tests completed"

# ============================================================================
# DOCKER TARGETS
# ============================================================================

docker-build: ## Build Docker image
	@echo "Building Docker image..."
	@$(MVN) clean package -Dquarkus.container-image.build=true --no-transfer-progress
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
# SECRETS MANAGEMENT
# ============================================================================

env-encrypt: ## Encrypt .env file to .env.enc using SOPS
	@echo "Encrypting .env file..."
	@if [ ! -f ".env" ]; then \
		echo "✗ .env file not found"; \
		exit 1; \
	fi
	@if [ ! -f ".sops.yaml" ]; then \
		echo "✗ .sops.yaml configuration not found"; \
		exit 1; \
	fi
	@sops --encrypt --input-type binary --output-type binary .env > .env.enc
	@echo "✓ .env encrypted to .env.enc"

env-decrypt: ## Decrypt .env.enc file to .env using SOPS
	@echo "Decrypting .env.enc file..."
	@if [ ! -f ".env.enc" ]; then \
		echo "✗ .env.enc file not found"; \
		exit 1; \
	fi
	@if [ ! -f ".sops.yaml" ]; then \
		echo "✗ .sops.yaml configuration not found"; \
		exit 1; \
	fi
	@sops --decrypt --input-type binary --output-type binary .env.enc > .env
	@echo "✓ .env.enc decrypted to .env"

# ============================================================================
# UTILITY RULES
# ============================================================================

# Ensure JAR file exists before running
$(JAR_FILE):
	@if [ ! -f "$(JAR_FILE)" ]; then \
		echo "JAR file not found. Building..."; \
		$(MAKE) build; \
	fi
