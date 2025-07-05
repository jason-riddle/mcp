# Modern Makefile best practices
.PHONY: help clean build dev run format checkstyle test test-watch test-integration docker-build docker-run docker-clean update-readme gcloud-deploy gcloud-push gcloud-proxy gcloud-logs gcloud-status
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

# ============================================================================
# GOOGLE CLOUD CONFIGURATION
# ============================================================================
GCLOUD_PROJECT := jasons-mcp-server-20250705
GCLOUD_REGION := us-central1
GCLOUD_SERVICE := jasons-mcp-server
GCLOUD_REGISTRY := $(GCLOUD_REGION)-docker.pkg.dev/$(GCLOUD_PROJECT)/mcp-servers
GCLOUD_IMAGE := $(GCLOUD_REGISTRY)/$(PROJECT_NAME):latest

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
	@echo "Google Cloud:"
	@echo "  gcloud-push   - Build and push image to Artifact Registry"
	@echo "  gcloud-deploy - Deploy to Cloud Run"
	@echo "  gcloud-proxy  - Start Cloud Run proxy for local access"
	@echo "  gcloud-logs   - View Cloud Run logs"
	@echo "  gcloud-status - Check Cloud Run service status"
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
# GOOGLE CLOUD TARGETS
# ============================================================================

## Build and push Docker image to Artifact Registry
gcloud-push: docker-build
	@echo "Tagging image for Artifact Registry..."
	@docker tag registry.fly.io/$(PROJECT_NAME)/$(PROJECT_NAME):latest $(GCLOUD_IMAGE)
	@echo "Pushing to Artifact Registry..."
	@docker push $(GCLOUD_IMAGE)
	@echo "✓ Image pushed to $(GCLOUD_IMAGE)"

## Deploy to Google Cloud Run
gcloud-deploy:
	@echo "Deploying to Cloud Run..."
	@gcloud run deploy $(GCLOUD_SERVICE) \
		--image $(GCLOUD_IMAGE) \
		--region $(GCLOUD_REGION) \
		--port 8080 \
		--no-allow-unauthenticated \
		--min-instances 0 \
		--max-instances 1 \
		--memory 512Mi \
		--cpu 1 \
		--timeout 300
	@echo "✓ Deployed to Cloud Run"
	@echo "Service URL: $$(gcloud run services describe $(GCLOUD_SERVICE) --region $(GCLOUD_REGION) --format='value(status.url)')"

## Start Cloud Run proxy for local access
gcloud-proxy:
	@echo "Starting Cloud Run proxy..."
	@echo "MCP endpoint will be available at: http://localhost:3000/v1/mcp/sse"
	@echo "Press Ctrl+C to stop"
	@gcloud run services proxy $(GCLOUD_SERVICE) --region $(GCLOUD_REGION) --port 3000

## View Cloud Run service logs
gcloud-logs:
	@echo "Viewing Cloud Run logs..."
	@gcloud run services logs read $(GCLOUD_SERVICE) --region $(GCLOUD_REGION) --limit 50

## Check Cloud Run service status
gcloud-status:
	@echo "Cloud Run service status:"
	@gcloud run services describe $(GCLOUD_SERVICE) --region $(GCLOUD_REGION) --format="table(metadata.name,status.url,status.conditions[0].status,spec.template.spec.containers[0].image)"

# ============================================================================
# UTILITY RULES
# ============================================================================

# Ensure JAR file exists before running
$(JAR_FILE):
	@if [ ! -f "$(JAR_FILE)" ]; then \
		echo "JAR file not found. Building..."; \
		$(MAKE) build; \
	fi
