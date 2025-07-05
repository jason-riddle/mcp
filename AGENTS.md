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

# Google Cloud Build (CI/CD)
make cloud-build                  # Submit manual build to Cloud Build
make cloud-build-status          # Check recent build status
make cloud-build-logs            # View latest build logs
make cloud-build-trigger-create  # Create GitHub trigger
make cloud-build-trigger-list    # List build triggers

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
- **Scaling**: Auto-scaling from 0 to 1 instances
- **Resources**: 512Mi memory, 1 CPU, 300s timeout

### Local Access

Use the Cloud Run proxy for secure local MCP client connections:

```bash
# Start proxy
make gcloud-proxy

# Configure MCP client
# URL: http://localhost:3000/v1/mcp/sse
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

### Configuration

Configure via `application.properties`:

```properties
# Memory file path (default: memory.jsonl)
memory.file.path=memory.jsonl

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

#### Cloud Build Tags

Cloud Build generates unique, traceable tags combining multiple identifiers:

**Tag Format:** `build-{BUILD_ID}-{MAVEN_VERSION}-{TIMESTAMP}`

**Example:** `build-ffa31445-4f67-41aa-97cf-cd66e1279eba-0.0.1-SNAPSHOT-20250705-163119`

**Components:**
- `BUILD_ID` - Google Cloud Build unique identifier
- `MAVEN_VERSION` - Extracted from `pom.xml` using `mvn help:evaluate`
- `TIMESTAMP` - ISO format: `YYYYMMDD-HHMMSS` (UTC)

#### Tag Generation Process

Cloud Build uses a two-step process in `cloudbuild.yaml`:

1. **Generate Tag:** Extract Maven version and create timestamp
2. **Build & Push:** Use unique tag for container image and deployment

```bash
# Extract version and create unique tag
MAVEN_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
TIMESTAMP=$(date -u +%Y%m%d-%H%M%S)
UNIQUE_TAG="build-${BUILD_ID}-${MAVEN_VERSION}-${TIMESTAMP}"

# Build with unique tag
mvn clean package -DskipTests=true \
  -Dquarkus.container-image.tag="${UNIQUE_TAG}"
```

#### Best Practices Implemented

- ✅ **Unique identification** - Every Cloud Build has a unique, non-colliding tag
- ✅ **Traceability** - Can trace image back to specific build, version, and time
- ✅ **Reproducibility** - Timestamp ensures consistent identification across environments
- ✅ **Version control** - Maven version included for release tracking
- ✅ **Build correlation** - Cloud Build ID links image to CI/CD logs and artifacts

#### Rollback Strategy

To rollback to a previous deployment:

```bash
# Find desired build tag
make cloud-build-status

# Deploy specific tag
gcloud run deploy jasons-mcp-server \
  --image=us-central1-docker.pkg.dev/PROJECT_ID/mcp-servers/jasons-mcp-server:build-ABC123-0.0.1-SNAPSHOT-20250705-120000 \
  --region=us-central1
```

### Documentation Generation

```bash
# Update README.md with latest tool and resource documentation
make update-readme

# Or run directly
python scripts/update-docs.py
```

## Google Cloud Build

### Overview

Google Cloud Build provides automated CI/CD for the MCP server with container building and deployment to Cloud Run.

**Key Features:**
- Automated build on GitHub commits
- Parallel build and deployment steps
- Artifact Registry integration
- Build caching for performance
- Comprehensive logging and monitoring

### Cloud Build Configuration

The project uses `cloudbuild.yaml` for build configuration:

```yaml
# Two-step pipeline:
# 1. Maven build with Quarkus container image
# 2. Deploy to Cloud Run
steps:
  - name: 'maven:3.9.9-eclipse-temurin-17'
    entrypoint: 'mvn'
    args: ['clean', 'package', '-DskipTests=true', ...]

  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: 'gcloud'
    args: ['run', 'deploy', ...]
```

### Build Management Commands

#### Manual Builds

```bash
# Submit manual build
make cloud-build

# Check build status
make cloud-build-status

# View build logs
make cloud-build-logs
```

#### Automated Triggers

```bash
# Create GitHub trigger for automatic builds
make cloud-build-trigger-create

# List existing triggers
make cloud-build-trigger-list

# Direct gcloud commands
gcloud builds triggers create github \
  --repo-name=mcp \
  --repo-owner=USERNAME \
  --branch-pattern="^main$" \
  --build-config=cloudbuild.yaml
```

### Build Process Flow

1. **Source Upload**: Code archived and uploaded to Cloud Storage
2. **Maven Build**: Dependencies downloaded, code compiled, tests skipped
3. **Container Build**: Quarkus container image built using JIB
4. **Image Push**: Container pushed to Artifact Registry
5. **Cloud Run Deploy**: New revision deployed with zero-downtime
6. **Health Check**: Service verified and traffic routed

### Build Optimization

#### Performance Settings

```yaml
# High-performance machine for faster builds
options:
  machineType: 'E2_HIGHCPU_8'
  diskSizeGb: 100

# Extended timeout for complex builds
timeout: '1200s'
```

#### Caching Strategy

- **Maven Dependencies**: Cached in container layers
- **Docker Layers**: Reused across builds
- **Quarkus Build Cache**: Optimizes compilation

#### .gcloudignore Configuration

```bash
# Excludes unnecessary files from upload
.git/
target/
*.log
docs/
staging/
```

### Build URLs and Resources

- **Cloud Build Console**: https://console.cloud.google.com/cloud-build/builds
- **Artifact Registry**: https://console.cloud.google.com/artifacts
- **Build History**: `gcloud builds list --format="table(id,status,createTime,duration)"`
- **Build Logs**: `gcloud builds log BUILD_ID`

### Trigger Configuration

#### GitHub Integration

```bash
# Connect GitHub repository
gcloud builds triggers create github \
  --repo-name=mcp \
  --repo-owner=jason-riddle \
  --branch-pattern="^main$" \
  --build-config=cloudbuild.yaml \
  --description="Automatic build and deploy on main branch push"
```

#### Trigger Filters

- **Branch**: `^main$` (main branch only)
- **Included Files**: All files (no filter)
- **Build Config**: `cloudbuild.yaml`
- **Substitutions**: `PROJECT_ID`, `BUILD_ID` variables

### Service Accounts and Permissions

Cloud Build service account requires:

```bash
# Grant necessary permissions
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:PROJECT_NUMBER@cloudbuild.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:PROJECT_NUMBER@cloudbuild.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:PROJECT_NUMBER@cloudbuild.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"
```

### Troubleshooting

#### Common Issues

1. **Authentication errors**: Ensure `gcloud auth login` is current
2. **Billing not enabled**: Link a billing account to the project
3. **Permission denied**: Verify IAM roles (Cloud Run Admin, Artifact Registry Writer)
4. **Proxy connection fails**: Check port 3000 availability and service status

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

##### Docker Authentication for Artifact Registry

**Issue**: Push operations fail even with valid gcloud authentication

**Solution**: Configure Docker authentication for Artifact Registry explicitly:

```bash
# Configure Docker to use gcloud as credential helper for Artifact Registry
gcloud auth configure-docker us-central1-docker.pkg.dev

# Verify credential helper is configured
cat ~/.docker/config.json
# Should include: "us-central1-docker.pkg.dev": "gcloud"
```

**Note**: Unlike Container Registry (`*.gcr.io`), Artifact Registry requires explicit host configuration.

##### Build vs Push Distinction

The Makefile uses different approaches:

```bash
# docker-build: Builds local image using Quarkus container-image extension
make docker-build

# gcloud-push: Tags local image and pushes to Artifact Registry
make gcloud-push  # Runs docker-build first, then tags and pushes

# Complete deployment workflow
make gcloud-push && make gcloud-deploy
```

#### Debug Commands

```bash
# Check authentication and project
gcloud auth list
gcloud config list

# Verify service deployment
gcloud run services list --region us-central1

# Test proxy connection
curl -I http://localhost:3000/v1/mcp/sse

# Check Artifact Registry repository
gcloud artifacts repositories describe mcp-servers --location=us-central1

# Verify Docker authentication
docker system info | grep -A5 "Registry Mirrors"

# Test container image build locally
./mvnw clean package -Dquarkus.container-image.build=true

# Check running containers
docker images | grep jasons-mcp-server

# View detailed Cloud Run service info
gcloud run services describe jasons-mcp-server --region us-central1 --format="export"
```

#### Quarkus Development Issues

##### Integration Test Connection Failures

**Symptoms**: Tests show `HttpClosedException: Connection was closed` during build

**Explanation**: These are normal integration test connection attempts to verify SSE endpoints. They don't indicate build failures if the main build succeeds.

**Resolution**: The errors are expected during testing and don't affect deployment. Monitor for actual build failure messages in Maven output.

##### JIB Multi-platform Warnings

**Symptoms**: Warnings about base image digests and multi-platform builds

**Solution**: These are suppressed in `application.properties`:

```properties
# Suppress JIB processor warning messages
quarkus.log.category."io.quarkus.container.image.jib.deployment.JibProcessor".level=ERROR
```

### Cloud Build Troubleshooting

#### Build Failures

##### Maven Dependency Download Issues

**Symptoms**: Builds fail with "Could not resolve dependencies" or network timeouts

**Solution**:
```bash
# Check network connectivity in build environment
# Increase timeout in cloudbuild.yaml
timeout: '1800s'

# Use dependency caching
options:
  machineType: 'E2_HIGHCPU_8'  # More powerful machine
```

##### Container Image Build Failures

**Symptoms**: "Failed to build quarkus application" or "container-image.tag is empty"

**Solution**: Ensure proper substitution variables:
```yaml
# Use BUILD_ID instead of SHORT_SHA for reliability
'-Dquarkus.container-image.tag=build-${BUILD_ID}'
```

##### Permission Denied Errors

**Symptoms**: "Permission denied" when pushing to Artifact Registry or deploying to Cloud Run

**Solution**: Verify service account permissions:
```bash
# Check Cloud Build service account permissions
gcloud projects get-iam-policy PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members~cloudbuild"

# Grant missing permissions
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:PROJECT_NUMBER@cloudbuild.gserviceaccount.com" \
  --role="roles/run.admin"
```

#### Build Performance Issues

##### Slow Build Times

**Root Cause**: Insufficient resources or repeated dependency downloads

**Solutions**:
1. **Increase Machine Resources**:
   ```yaml
   options:
     machineType: 'E2_HIGHCPU_32'  # For complex builds
     diskSizeGb: 200
   ```

2. **Optimize .gcloudignore**:
   ```bash
   # Exclude large unnecessary files
   target/
   .git/
   node_modules/
   staging/
   ```

3. **Parallel Steps**: Use step dependencies to parallelize where possible

##### Build Timeouts

**Issue**: Builds exceed default timeout limits

**Solution**: Increase timeout in cloudbuild.yaml:
```yaml
timeout: '3600s'  # 1 hour for complex builds
```

#### Debugging Commands

```bash
# View detailed build logs
gcloud builds log BUILD_ID

# Monitor build in real-time
gcloud builds log BUILD_ID --stream

# List recent builds with timing
gcloud builds list --limit=10 --format="table(id,status,createTime,duration)"

# Check build configuration
gcloud builds describe BUILD_ID

# Validate cloudbuild.yaml syntax
gcloud builds submit --config cloudbuild.yaml --dry-run

# Test individual build steps locally (for debugging)
docker run --rm -v "$(pwd)":/workspace -w /workspace \
  maven:3.9.9-eclipse-temurin-17 \
  mvn clean package -DskipTests=true
```

#### Trigger Issues

##### GitHub Integration Problems

**Issue**: Trigger not firing on repository pushes

**Solutions**:
1. **Verify Repository Connection**:
   ```bash
   gcloud builds triggers list --format="table(name,status,github.name)"
   ```

2. **Check Branch Pattern**:
   ```bash
   # Ensure pattern matches your branch name
   --branch-pattern="^main$"  # Exact match
   --branch-pattern=".*"      # All branches
   ```

3. **Re-authenticate GitHub**:
   ```bash
   # May need to reconnect GitHub integration in Cloud Console
   ```

##### Webhook Delivery Failures

**Issue**: GitHub webhooks not reaching Cloud Build

**Debugging**:
1. Check GitHub webhook delivery history
2. Verify Cloud Build API is enabled
3. Check organization policies blocking external connections

#### Resource Quotas and Limits

```bash
# Check Cloud Build quotas
gcloud compute project-info describe \
  --format="table(quotas.metric,quotas.limit,quotas.usage)" \
  --filter="quotas.metric~cloudbuild"

# Check concurrent build limits
gcloud builds list --filter="status=WORKING" --format="table(id,createTime)"

# Regional resource availability
gcloud compute regions list --format="table(name,status)" \
  --filter="name~us-central"
```
