# STDIO Integration Test Race Condition Issue

## Problem Summary

The Quarkus MCP server integration tests fail consistently due to a race condition in STDIO transport where the MCP server subprocess receives EOF and shuts down before tool execution completes. The `DefaultMcpClient.executeTool()` method from LangChain4j receives null/error responses at line 145, indicating subprocess lifecycle management issues.

## Error Details

### Primary Error Message
```
java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because the return value of "com.fasterxml.jackson.databind.JsonNode.get(String)" is null
	at dev.langchain4j.mcp.client.DefaultMcpClient.executeTool(DefaultMcpClient.java:145)
```

### Complete Error Stack Trace
```
[ERROR] McpServerStdioIntegrationTest.shouldInitializeMcpConnection:117 » NullPointer Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because the return value of "com.fasterxml.jackson.databind.JsonNode.get(String)" is null
[ERROR] McpServerStdioIntegrationTest.shouldExecuteCreateEntitiesAndReadGraph:143 » NullPointer Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because the return value of "com.fasterxml.jackson.databind.JsonNode.get(String)" is null
[ERROR] McpServerStdioIntegrationTest.shouldExecuteSearchNodesTool:179 » NullPointer Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because the return value of "com.fasterxml.jackson.databind.JsonNode.get(String)" is null
[ERROR] McpServerStdioIntegrationTest.shouldHandleSubprocessTermination:199 » NullPointer Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because the return value of "com.fasterxml.jackson.databind.JsonNode.get(String)" is null
[ERROR] McpServerStdioIntegrationTest.shouldExecuteFullEntityCrudLifecycle:253 » NullPointer Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because the return value of "com.fasterxml.jackson.databind.JsonNode.get(String)" is null
```

## Root Cause Analysis

### Detailed Debug Log Analysis

From debug logs captured during manual testing (`debug.log`):

#### Successful Manual Test (with sleep delay):
```
2025-07-06 14:39:25,164 DEBUG [io.qua.mcp.ser.run.ToolMessageHandler] (vert.x-eventloop-thread-0) Call tool memory_read_graph [id: 3]
2025-07-06 14:39:25,166 INFO  [io.quarkus] (main) jasons-mcp-server 0.0.1-SNAPSHOT on JVM (powered by Quarkus 3.24.2) started in 0.469s. Listening on: http://0.0.0.0:8080
2025-07-06 14:39:25,167 INFO  [io.quarkus] (main) Installed features: [cdi, mcp-server-stdio, rest, rest-jackson, smallrye-context-propagation, vertx]
2025-07-06 14:39:26,661 DEBUG [io.qua.mcp.ser.std.run.StdioMcpMessageHandler] (pool-5-thread-1) EOF received, exiting
2025-07-06 14:39:26,662 DEBUG [io.qua.run.Application] (main) Stopping application
```

**Result**: Tool executed successfully, returned proper JSON response:
```json
{"jsonrpc":"2.0","id":3,"result":{"isError":false,"content":[{"text":"{\"entities\":[{\"type\":\"entity\",\"name\":\"TestPrefs\",\"entityType\":\"preferences\",\"observations\":[\"Preference observation\"]},{\"type\":\"entity\",\"name\":\"TestUser\",\"entityType\":\"person\",\"observations\":[\"Test observation 1\",\"Test observation 2\"]}],\"relations\":[]}","type":"text"}]}}
```

#### Failed Test (without delay):
```
2025-07-06 14:37:40,263 DEBUG [io.qua.mcp.ser.run.ToolMessageHandler] (vert.x-eventloop-thread-0) Call tool memory_read_graph [id: 3]
2025-07-06 14:37:40,264 INFO  [io.quarkus] (main) jasons-mcp-server 0.0.1-SNAPSHOT on JVM (powered by Quarkus 3.24.2) started in 0.507s.
2025-07-06 14:37:40,217 DEBUG [io.qua.mcp.ser.std.run.StdioMcpMessageHandler] (pool-5-thread-1) EOF received, exiting
2025-07-06 14:37:40,265 DEBUG [io.qua.run.Application] (main) Stopping application
2025-07-06 14:37:40,289 ERROR [io.qua.mcp.ser.run.ToolMessageHandler] (executor-thread-1) Unable to call tool memory_read_graph [Error Occurred After Shutdown]: java.lang.NullPointerException: Cannot invoke "io.smallrye.context.SmallRyeContextManager.defaultThreadContext()" because the return value of "io.smallrye.context.SmallRyeContextManagerProvider.getManager()" is null
```

### Key Insights from Log Analysis

1. **Race Condition Confirmed**: Tool execution begins (`Call tool memory_read_graph [id: 3]`) but application shutdown starts simultaneously
2. **EOF Triggers Shutdown**: `StdioMcpMessageHandler` receives EOF immediately, triggering application shutdown
3. **CDI Context Lost**: `SmallRyeContextManagerProvider.getManager()` returns null because CDI container has been shut down
4. **Timing Dependency**: Manual test with 2-second sleep delay works perfectly, confirming the MCP protocol implementation is sound

## Technical Context

### MCP Protocol Implementation

#### Quarkus MCP Server Configuration
**File**: `src/main/resources/application.properties`
```properties
# STDIO transport is enabled by default when SSE is not configured
# quarkus.mcp.server.memory.stdio.enabled=true

# Configure shutdown timeout to prevent race conditions
quarkus.shutdown.timeout=30s
# Delay shutdown to allow tool execution to complete
quarkus.shutdown.delay-enabled=true
```

#### Tool Implementation
**File**: `src/main/java/com/jasonriddle/mcp/McpMemoryTools.java`
```java
@Tool(name = "memory_read_graph", description = "Read the entire knowledge graph")
public MemoryGraph readGraph() {
    return memoryService.readGraph();
}
```

### LangChain4j MCP Client Configuration

**Integration Test Setup** (`src/test/java/com/jasonriddle/mcp/McpServerStdioIntegrationTest.java`):
```java
// Configure STDIO transport to spawn MCP server subprocess
McpTransport transport = new StdioMcpTransport.Builder()
        .command(List.of("java", "-jar", jarPath))
        .logEvents(true) // Enable logging for debugging
        .build();

// Create MCP client with configured transport
mcpClient = new DefaultMcpClient.Builder()
        .clientName("stdio-integration-test-client")
        .toolExecutionTimeout(CLIENT_TIMEOUT) // Duration.ofSeconds(10)
        .transport(transport)
        .build();
```

**Tool Execution**:
```java
String result = mcpClient.executeTool(ToolExecutionRequest.builder()
        .name("memory_read_graph")
        .arguments("{}")
        .build());
```

## Research and Documentation Sources

### Official Documentation Sources

1. **Quarkus MCP Server Documentation** (Found in local codebase)
   - `docs/quarkus/mcp/quarkus-docs-mcp-server-dev-index.md`
   - **Key Finding**: "When a server feature is executed, the CDI request context is active and a Vert.x duplicated context is created"
   - **STDIO Transport**: "The stdio transport starts an MCP server as a subprocess and communicates over standard in and out"

2. **Quarkus Blog Posts** (Found in local codebase)
   - `docs/quarkus/mcp/quarkus-blog-mcp-server.md`
   - **Best Practice**: "Enable file logging as without it we would not be able to see any logs from the server as standard input/output is reserved for the MCP protocol"

### Web Research Sources

3. **Quarkus Issue Tracker Search Results**:
   - **URL**: `https://github.com/quarkusio/quarkus/issues/43877`
   - **Issue**: "RabbitMQClientImpl [Error Occurred After Shutdown]: java.lang.NullPointerException"
   - **Pattern**: Same `SmallRyeContextManagerProvider.getManager()` null error during shutdown

4. **Model Context Protocol Documentation**:
   - **URL**: `https://modelcontextprotocol.io/docs/tools/debugging`
   - **Key Quote**: "Tool errors should be reported within the result object, not as MCP protocol-level errors"
   - **STDIO Requirement**: "Local MCP servers should not log messages to stdout (standard out), as this will interfere with protocol operation"

5. **Cursor Community Forum**:
   - **URL**: `https://forum.cursor.com/t/mcp-1-0-0-startup-race-condition-causes-no-server-info-found-and-broken-tool-offerings-works-in-0-49/100327`
   - **Issue**: "MCP 1.0.0: Startup Race Condition Causes 'No server info found'"
   - **Relevance**: Confirms race conditions are a known issue in MCP implementations

### Dependency Information

- **LangChain4j MCP Client**: `dev.langchain4j:langchain4j-mcp:1.0.0-alpha1`
- **Quarkus MCP Server**: `io.quarkiverse.mcp:quarkus-mcp-server-stdio:1.3.1`
- **Quarkus Version**: `3.24.2`

## Previous Attempts and Solutions Tried

### 1. Tool Name Corrections ✅ **SOLVED**
**Problem**: Integration tests were calling incorrect tool names
- `read_graph` → `memory_read_graph`
- `create_entities` → `memory_create_entities`
- `search_nodes` → `memory_search_nodes`

**Solution**: Updated all tool calls in integration tests to use correct names with `memory_` prefix

**Verification**:
```bash
# Manual verification showed correct tools are available:
{"tools":[{"name":"memory_read_graph","description":"Read the entire knowledge graph"}, ...]}
```

### 2. Shutdown Configuration ⚠️ **PARTIALLY EFFECTIVE**
**Attempted**: Added Quarkus shutdown timeout and delay configuration
```properties
quarkus.shutdown.timeout=30s
quarkus.shutdown.delay-enabled=true
```

**Result**: Manual testing with sleep delay works, but integration tests still fail

### 3. Enhanced Logging ✅ **DIAGNOSTIC SUCCESS**
**Configuration**:
```properties
quarkus.log.level=INFO
quarkus.log.category."com.jasonriddle.mcp".level=DEBUG
quarkus.log.category."io.quarkiverse.mcp".level=DEBUG
quarkus.log.category."dev.langchain4j.mcp".level=DEBUG
quarkus.log.console.stderr=true
```

**Result**: Successfully identified the race condition timing and CDI context loss

### 4. Client Timeout Adjustment ❌ **INEFFECTIVE**
**Attempted**: Reduced client timeout from 30 seconds to 10 seconds
```java
private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(10);
```

**Result**: No improvement, issue occurs before timeout

## Detailed Analysis of LangChain4j MCP Client

### Subprocess Management in StdioMcpTransport

Based on error location `DefaultMcpClient.executeTool(DefaultMcpClient.java:145)`, the issue occurs in LangChain4j's MCP client when parsing the JSON response from the server.

**Expected Flow**:
1. LangChain4j spawns Java subprocess with MCP server JAR
2. Client sends JSON-RPC initialize message
3. Client sends JSON-RPC tool execution request
4. Server processes request and sends JSON-RPC response
5. Client parses response at line 145 in `DefaultMcpClient.executeTool()`

**Actual Flow**:
1. ✅ LangChain4j spawns Java subprocess successfully
2. ✅ Client sends initialize message successfully
3. ✅ Client sends tool execution request successfully
4. ❌ Server begins processing but receives EOF, triggering shutdown
5. ❌ Client receives null/error response, causing NullPointerException at line 145

### Manual vs Integration Test Difference

**Manual Test (Working)**:
```bash
(echo 'initialize_msg'; echo 'initialized_notification'; echo 'tool_call_msg'; sleep 2) | java -jar target/quarkus-app/quarkus-run.jar
```
- Sleep delay keeps input stream open
- Tool execution completes before shutdown
- Server returns proper JSON response

**Integration Test (Failing)**:
```java
mcpClient.executeTool(ToolExecutionRequest.builder()
    .name("memory_read_graph")
    .arguments("{}")
    .build());
```
- LangChain4j manages subprocess lifecycle
- No explicit delay mechanism
- Subprocess receives EOF immediately after sending messages
- Race condition between tool execution and shutdown

## Potential Solutions Analysis

### 1. Connection Keepalive in LangChain4j Client ⭐ **HIGH PRIORITY**

**Research Needed**: Investigate `StdioMcpTransport.Builder` options for connection management

**Potential Configuration**:
```java
McpTransport transport = new StdioMcpTransport.Builder()
        .command(List.of("java", "-jar", jarPath))
        .logEvents(true)
        // Look for keepalive or connection persistence options
        .build();
```

**Investigation Required**:
- Review LangChain4j MCP client source code for configuration options
- Check if there are builder methods for subprocess lifecycle management
- Investigate if client has connection pooling or persistent connection options

### 2. Retry Logic for Tool Execution ⭐ **MEDIUM PRIORITY**

**Implementation Approach**:
```java
public String executeToolWithRetry(ToolExecutionRequest request, int maxRetries) {
    for (int i = 0; i < maxRetries; i++) {
        try {
            return mcpClient.executeTool(request);
        } catch (NullPointerException e) {
            if (i == maxRetries - 1) throw e;
            // Wait and retry
            Thread.sleep(1000);
        }
    }
}
```

**Limitations**: May mask underlying race condition rather than solving it

### 3. Alternative Test Approach ⭐ **LOW PRIORITY**

**Options**:
- Use HTTP/SSE transport for integration tests instead of STDIO
- Mock the MCP client to test business logic without subprocess
- Use TestContainers to manage subprocess lifecycle

**Trade-offs**: Would not test actual deployment scenario (STDIO subprocess)

### 4. Enhanced Client Configuration ⭐ **HIGH PRIORITY**

**Research Areas**:
- Increase tool execution timeout beyond 10 seconds
- Configure subprocess startup delay
- Add client-side connection health checks
- Implement custom error handling for null responses

## Related Known Issues

### Quarkus Shutdown Race Conditions

**Issue Pattern**: Multiple reports of `SmallRyeContextManagerProvider.getManager()` returning null during shutdown
- **RabbitMQ**: Issue #43877 - Similar race condition during connection callbacks
- **General**: Issue #33588 - "Failed to stop Quarkus [Error Occurred After Shutdown]"
- **Arc Container**: Issue #14980 - "NullPointerException in ArcContextProvider#deactivateRequestContext on application shutdown"

**Common Pattern**: Components trying to access SmallRye context propagation after shutdown has begun

### MCP Protocol Compliance Issues

**Stdout Contamination**: MCP protocol requires all non-protocol output to go to stderr
- **Current Status**: ✅ Configured correctly with `quarkus.log.console.stderr=true`
- **Verification**: Manual testing shows proper JSON-RPC responses on stdout

**Connection Management**: STDIO transport expects persistent connection until client closes
- **Current Issue**: ❌ Server receives EOF and shuts down prematurely
- **Expected**: Server should stay alive until client explicitly terminates connection

## Next Steps for Resolution

### Immediate Actions Required

1. **LangChain4j Source Code Investigation**
   - Download/examine LangChain4j MCP client source code
   - Identify `StdioMcpTransport` configuration options
   - Look for connection persistence or keepalive mechanisms
   - Check if there are undocumented builder options

2. **Subprocess Lifecycle Analysis**
   - Add logging to track when LangChain4j closes subprocess stdin
   - Determine if client is closing connection prematurely
   - Test with different tool execution timeouts

3. **Alternative Client Configuration**
   - Test with longer tool execution timeouts (30+ seconds)
   - Try different LangChain4j MCP client versions if available
   - Investigate if client has process management options

### Research Questions to Answer

1. **How does LangChain4j manage subprocess stdin/stdout lifecycle?**
2. **Are there configuration options to keep subprocess connections alive?**
3. **Does the MCP specification define connection keepalive requirements for STDIO transport?**
4. **Are there examples of successful STDIO integration tests with LangChain4j MCP client?**

### Validation Criteria

**Solution Success Indicators**:
- ✅ All 5 integration tests pass consistently
- ✅ No race condition errors in logs
- ✅ Tool execution completes before shutdown
- ✅ Proper JSON responses received by client
- ✅ No CDI context null pointer exceptions

## Status Summary

**Current State**:
- ✅ MCP server implementation is correct and functional
- ✅ Manual STDIO testing works with proper timing
- ✅ Tool names and protocols are correctly implemented
- ❌ Integration test framework has subprocess lifecycle race condition

**Primary Blocker**: LangChain4j MCP client subprocess management causing premature EOF and server shutdown

**Risk Level**: **Medium** - Core functionality works, only automated testing affected

**Confidence in Resolution**: **High** - Issue is well-understood and isolated to test framework configuration

---

# DETAILED TROUBLESHOOTING SESSION (2025-07-06)

## Session Summary
**Duration**: ~1.5 hours
**Focus**: Root cause analysis and attempted fix for integration test failures
**Outcome**: ❌ **PROBLEM NOT FIXED** - Identified fundamental LangChain4j alpha version bug
**Key Discovery**: Issue is NOT a race condition but missing MCP protocol `initialized` notification

## What We Did This Session

### 1. Initial Problem Analysis
**Command Used**: `make test-integration`
**Result**: All 5 integration tests failed with same error pattern
**Key Finding**: Error message revealed "Client not initialized yet" rather than race condition

#### Error Pattern Change
**Previous Understanding** (from earlier analysis):
- Race condition between tool execution and shutdown
- CDI context loss during shutdown
- EOF triggering premature server shutdown

**New Understanding** (from this session):
- MCP protocol violation: missing `initialized` notification
- LangChain4j client bug in alpha version
- Server correctly rejecting tool calls per MCP specification

### 2. Web Research on MCP Protocol Requirements

#### Search Query 1: `"MCP Model Context Protocol initialization notification sequence Java LangChain4j"`
**Key Sources Found**:
- https://docs.langchain4j.dev/tutorials/mcp/
- https://blog.marcnuri.com/connecting-to-mcp-server-with-langchain4j
- https://medium.com/javarevisited/model-context-protocol-mcp-basics-and-building-mcp-server-and-client-in-java-and-spring-ee79a21117d9

**Critical Discovery**: MCP protocol requires specific handshake sequence:
1. Client sends `initialize` request
2. Server responds with `initialize` response
3. **Client MUST send `initialized` notification** ⭐ **MISSING STEP**
4. Only then can tools be called

#### Search Query 2: `""initialized notification" MCP protocol specification client server handshake"`
**Key Sources Found**:
- https://github.com/anthropics/claude-code/issues/1604 - "Missing MCP Notifications/Initialized in Handshake Protocol"
- https://modelcontextprotocol.io/specification/2025-03-26/basic/lifecycle

**Critical Quote Found**:
> "After successful initialization, the client MUST send an initialized notification to indicate it is ready to begin normal operations"

**Known Issue Confirmed**:
> "Claude Code's MCP client doesn't send the required 'notifications/initialized' message after the initialize handshake, causing properly-implemented MCP servers to reject tool requests"

### 3. LangChain4j Version Analysis
**Command Used**: `./mvnw dependency:tree | grep -i langchain4j`
**Result**: Using `dev.langchain4j:langchain4j-mcp:jar:1.0.0-alpha1:test`

#### Search Query 3: `"langchain4j-mcp" "1.0.0-alpha1" DefaultMcpClient initialization bug issue`
**Key Sources Found**:
- https://github.com/langchain4j/langchain4j/issues/2421 - "MCP response JSON parsing issues for errors"
- https://github.com/langchain4j/langchain4j/issues/2922 - "It is not possible to use the local tool and the MCP tool simultaneously"
- https://github.com/langchain4j/langchain4j/issues/2709 - "MCP error in resolving schema specification"

**Key Finding**: Multiple MCP-related bugs reported in langchain4j, confirming alpha version instability

### 4. Log Analysis Deep Dive

#### Integration Test Logs (Failed)
```
2025-07-06 14:54:22,492 DEBUG [dev.lan.mcp.cli.tra.std.ProcessIOHandler] (main) > {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"roots":{"listChanged":false}},"clientInfo":{"name":"stdio-integration-test-client","version":"1.0"}}}

2025-07-06 14:54:22,921 DEBUG [dev.lan.mcp.cli.tra.std.ProcessIOHandler] (Thread-15) < {"jsonrpc":"2.0","id":0,"result":{"capabilities":{"resources":{},"logging":{},"tools":{},"prompts":{}},"serverInfo":{"name":"jasons-mcp-server","version":"0.0.1-SNAPSHOT"},"protocolVersion":"2024-11-05"}}

2025-07-06 14:54:24,950 DEBUG [dev.lan.mcp.cli.tra.std.ProcessIOHandler] (main) > {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"memory_read_graph","arguments":{}}}

2025-07-06 14:54:24,953 DEBUG [dev.lan.mcp.cli.tra.std.ProcessIOHandler] (Thread-15) < {"jsonrpc":"2.0","id":1,"error":{"code":-32603,"message":"Client not initialized yet [OTQ5MDJjOWUtYmE0NS00YTQ1LTg4MWEtNWQwZmZiMzA3NjRj]"}}
```

**Critical Observation**: NO `initialized` notification sent between `initialize` response and `tools/call` request

#### What Should Happen (Per MCP Spec)
```
1. > initialize request
2. < initialize response
3. > initialized notification  ⭐ **MISSING**
4. > tools/call request
5. < tools/call response
```

### 5. Attempted Fixes and Results

#### Fix Attempt 1: Increased Sleep Delay
**File Modified**: `src/test/java/com/jasonriddle/mcp/McpServerStdioIntegrationTest.java:94-95`
**Change**:
```java
// OLD: Thread.sleep(2000);
Thread.sleep(3000); // Give the client time to complete initialization
```
**Result**: ❌ Failed - Same "Client not initialized yet" error

#### Fix Attempt 2: Retry Logic Implementation
**File Modified**: `src/test/java/com/jasonriddle/mcp/McpServerStdioIntegrationTest.java:428-452`
**New Method Added**:
```java
private void waitForClientReady() throws Exception {
    final int maxRetries = 5;
    final long retryDelayMs = 2000;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                    .name("memory_read_graph")
                    .arguments("{}")
                    .build());

            if (result != null && result.length() > 0) {
                return;
            }
        } catch (Exception e) {
            if (attempt == maxRetries) {
                throw new RuntimeException("MCP client failed to initialize after " + maxRetries + " attempts", e);
            }
            Thread.sleep(retryDelayMs);
        }
    }
}
```

**Command Used**: `make test-integration`
**Result**: ❌ Failed - All 5 retry attempts received "Client not initialized yet"

**Final Error**:
```
[ERROR] McpServerStdioIntegrationTest.setUp:99->waitForClientReady:448 Runtime MCP client failed to initialize after 5 attempts
```

## What We Learned About the Problem

### 1. Root Cause Identification
**Initial Hypothesis** (from ISSUE.md): Race condition in subprocess lifecycle management
**Actual Root Cause**: LangChain4j MCP client v1.0.0-alpha1 bug - missing `initialized` notification

### 2. MCP Protocol Compliance
**Discovery**: Our Quarkus MCP server is correctly implementing MCP specification
**Evidence**: Server properly rejects tool calls with "Client not initialized yet" when notification missing
**Verification**: Manual testing with proper delays works (as documented in original ISSUE.md)

### 3. Alpha Version Software Risks
**Learning**: Using alpha versions in integration tests introduces reliability issues
**Impact**: Core functionality works, but automated testing framework is unreliable
**Decision Required**: Upgrade to stable version or implement workaround

### 4. Debugging Methodology Effectiveness
**Successful Techniques**:
- Debug log analysis with protocol-level detail
- Web search for protocol specifications
- Dependency version checking
- Error pattern comparison

**Less Effective**:
- Retry logic for protocol violations
- Sleep delays for missing protocol steps
- Timeout adjustments

## How We Learned This

### 1. Debug Log Analysis
**Tool**: `make test-integration` with debug logging enabled
**Configuration**:
```properties
quarkus.log.category."dev.langchain4j.mcp".level=DEBUG
```
**Key Insight**: Logs showed missing `initialized` notification in protocol sequence

### 2. Internet Research
**Search Strategy**: Progressive refinement from general to specific
- Start: "MCP initialization LangChain4j"
- Refine: "initialized notification MCP protocol"
- Focus: "langchain4j-mcp alpha version bugs"

**Critical Sources**:
- MCP specification documentation
- LangChain4j GitHub issues
- Technical blog posts with code examples

### 3. Code Analysis
**Files Examined**:
- `src/test/java/com/jasonriddle/mcp/McpServerStdioIntegrationTest.java`
- Dependency tree via Maven
- Protocol logs from failed tests

### 4. Comparative Analysis
**Method**: Compared working manual test vs failing integration test
**Tool**: Log pattern analysis
**Result**: Identified missing protocol step

## What We Tried

### 1. ✅ **Attempted**: Sleep Delay Increase
**Rationale**: Allow more time for initialization
**Implementation**: `Thread.sleep(3000)` in `setUp()` method
**Result**: No improvement - protocol issue not timing issue

### 2. ✅ **Attempted**: Retry Logic with Exception Handling
**Rationale**: Work around intermittent failures
**Implementation**: 5 retries with 2-second delays
**Result**: All retries failed with same protocol error

### 3. ✅ **Attempted**: Error Pattern Analysis
**Method**: Detailed log examination
**Result**: Identified exact missing protocol step

### 4. ❌ **Considered but Not Implemented**: Manual Notification Sending
**Reason**: Would require reflection/hacking into LangChain4j internals
**Risk**: Brittle and maintenance-heavy solution

### 5. ❌ **Considered but Not Implemented**: Server-Side Workaround
**Option**: Make server accept tool calls without `initialized` notification
**Risk**: Violates MCP protocol specification

## What's Left to Do

### Immediate Options (Priority Order)

#### 1. **Upgrade LangChain4j Version** ⭐ **HIGHEST PRIORITY**
**Command**: Check for newer versions
```bash
mvn versions:display-dependency-updates -Dincludes=dev.langchain4j:langchain4j-mcp
```
**Expected Outcome**: Newer version may fix initialization bug
**Risk**: Low - Standard dependency upgrade

#### 2. **Implement Server-Side Lenient Mode** ⭐ **MEDIUM PRIORITY**
**Approach**: Configure Quarkus MCP to accept tool calls without `initialized` notification
**Implementation**: Add configuration property for protocol strictness
**Risk**: Medium - Violates MCP specification but enables testing

#### 3. **Switch to HTTP/SSE Transport** ⭐ **MEDIUM PRIORITY**
**Rationale**: May not have same initialization bug
**Implementation**: Modify integration tests to use HTTP transport
**Risk**: Medium - Different deployment scenario than STDIO

#### 4. **Mock Integration Tests** ⭐ **LOW PRIORITY**
**Approach**: Mock MCP client, test business logic only
**Benefit**: Avoid transport layer bugs entirely
**Risk**: Low - but reduced integration test coverage

### Research Needed

1. **LangChain4j Version Survey**
   - Check if newer versions (1.0.0-beta1, 1.0.0-rc1) exist
   - Review changelog for MCP client fixes
   - Test with latest available version

2. **Alternative Client Libraries**
   - Investigate other Java MCP clients
   - Consider implementing minimal custom client
   - Evaluate Python MCP client for comparison testing

3. **Protocol Specification Deep Dive**
   - Review MCP specification for initialization requirements
   - Understand server-side configuration options
   - Document expected vs actual behavior

## Important Troubleshooting Steps/Commands Used

### 1. Integration Test Execution
```bash
make test-integration
# Comprehensive test run with debug logging
```

### 2. Dependency Analysis
```bash
./mvnw dependency:tree | grep -i langchain4j
# Identified alpha version: 1.0.0-alpha1
```

### 3. Package Building
```bash
./mvnw package -DskipTests=true
# Required before integration tests
```

### 4. Individual Test Attempts
```bash
./mvnw failsafe:integration-test -Dit.test=McpServerStdioIntegrationTest#shouldInitializeMcpConnection
# Attempted single test execution (skipped due to configuration)
```

### 5. Web Research Commands
Multiple targeted searches for:
- MCP protocol specifications
- LangChain4j known issues
- Alpha version bug reports

## Relevant Code Snippets and Line Numbers

### 1. Integration Test Setup (Lines 86-95)
**File**: `src/test/java/com/jasonriddle/mcp/McpServerStdioIntegrationTest.java`
```java
mcpClient = new DefaultMcpClient.Builder()
        .clientName("stdio-integration-test-client")
        .toolExecutionTimeout(CLIENT_TIMEOUT)
        .transport(transport)
        .build();

// Wait for client to be properly initialized
Thread.sleep(3000); // Increased from 2000
waitForClientReady(); // Added retry logic
```

### 2. Retry Logic Implementation (Lines 428-452)
**File**: `src/test/java/com/jasonriddle/mcp/McpServerStdioIntegrationTest.java`
```java
private void waitForClientReady() throws Exception {
    final int maxRetries = 5;
    final long retryDelayMs = 2000;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            String result = mcpClient.executeTool(ToolExecutionRequest.builder()
                    .name("memory_read_graph")
                    .arguments("{}")
                    .build());

            if (result != null && result.length() > 0) {
                return; // Success
            }
        } catch (Exception e) {
            if (attempt == maxRetries) {
                throw new RuntimeException("MCP client failed to initialize after " + maxRetries + " attempts", e);
            }
            Thread.sleep(retryDelayMs);
        }
    }
}
```

### 3. Error Location (External)
**File**: `dev.langchain4j.mcp.client.DefaultMcpClient.java:145`
**Error**: `NullPointerException` when parsing null JSON response
**Cause**: Server returns error response, client expects success response

### 4. Protocol Sequence (From Logs)
**Missing Notification**: Should occur between initialize response and tools/call
```java
// What happens:
> initialize request
< initialize response
> tools/call request ❌ FAILS

// What should happen:
> initialize request
< initialize response
> initialized notification ⭐ MISSING
> tools/call request ✅ SUCCESS
```

## URL Sources Used

### Official Documentation
1. **LangChain4j MCP Tutorial**: https://docs.langchain4j.dev/tutorials/mcp/
2. **MCP Specification**: https://modelcontextprotocol.io/specification/2025-03-26/basic/lifecycle
3. **LangChain4j GitHub**: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-mcp

### Issue Tracking
4. **Claude Code Issue**: https://github.com/anthropics/claude-code/issues/1604
5. **LangChain4j Bug #2421**: https://github.com/langchain4j/langchain4j/issues/2421
6. **LangChain4j Bug #2922**: https://github.com/langchain4j/langchain4j/issues/2922

### Technical Blogs
7. **Marc Nuri Blog**: https://blog.marcnuri.com/connecting-to-mcp-server-with-langchain4j
8. **Medium Guide**: https://medium.com/javarevisited/model-context-protocol-mcp-basics-and-building-mcp-server-and-client-in-java-and-spring-ee79a21117d9
9. **Guillaume Laforge Blog**: https://glaforge.dev/posts/2025/04/04/mcp-client-and-server-with-java-mcp-sdk-and-langchain4j/

### Related Issues
10. **Quarkus Issue #43877**: https://github.com/quarkusio/quarkus/issues/43877

## Status Update

### Current Problem Status
❌ **NOT FIXED** - Integration tests still fail
📊 **Progress**: Root cause definitively identified
🎯 **Next Action**: Upgrade LangChain4j dependency or implement workaround

### Risk Assessment Update
**Previous Risk Level**: Medium (subprocess lifecycle race condition)
**Current Risk Level**: **Low** (known alpha version bug with clear solutions)
**Reason**: Problem is external dependency issue, not our code implementation

### Implementation Confidence
**Previous Confidence**: High (race condition fix)
**Current Confidence**: **Very High** (dependency upgrade or protocol workaround)
**Reason**: Multiple viable solutions available, core MCP server implementation verified correct
