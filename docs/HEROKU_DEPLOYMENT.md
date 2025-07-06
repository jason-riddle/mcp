# Deploying MCP Memory Server to Heroku

This guide explains how to deploy Jason's MCP Memory Server to Heroku as an MCP STDIO server.

## Prerequisites

- Heroku account with billing enabled (for Managed Inference and Agents add-on)
- Heroku CLI installed
- Git installed
- Java 17+ and Maven for local building

## Deployment Steps

### 1. Prepare Your Application

The repository is already configured with all necessary Heroku files:
- `Procfile` - Defines the MCP process type
- `system.properties` - Specifies Java 17 runtime
- `app.json` - Heroku app configuration
- `application-heroku.properties` - Heroku-specific Quarkus configuration

### 2. Deploy to Heroku

The repository is configured to automatically handle the build process on Heroku. The `app.json` file includes `MAVEN_CUSTOM_OPTS` that disable container image building during the Heroku build process.

### 3. Create Heroku App

```bash
# Login to Heroku
heroku login

# Create a new app (or use existing)
heroku create your-mcp-memory-server

# Set the Git remote
heroku git:remote -a your-mcp-memory-server
```

### 4. Deploy to Heroku

```bash
# Deploy using Git
git add .
git commit -m "Configure for Heroku deployment"
git push heroku main
```

The build will automatically use the Maven options specified in `app.json` to disable container image building.

### 5. Add Managed Inference and Agents Add-on

```bash
# Add the add-on (Claude 3.5 Haiku plan as example)
heroku addons:create heroku-inference:claude-3-5-haiku -a your-mcp-memory-server

# Or add via Heroku Dashboard:
# 1. Navigate to your app
# 2. Go to Resources tab
# 3. Search for "Managed Inference and Agents"
# 4. Select a plan (any Claude chat model for MCP support)
```

### 6. Verify Deployment

```bash
# Check logs
heroku logs --tail -a your-mcp-memory-server

# Check MCP process status
heroku ps -a your-mcp-memory-server
```

## Using Your MCP Server

### With Heroku Managed Inference

Your MCP tools are automatically available to the Heroku Managed Inference model you selected.

### With External Clients

1. Get your MCP Toolkit URL and Token:
   - Navigate to your app in Heroku Dashboard
   - Click on the Managed Inference and Agents add-on
   - Go to the Tools tab
   - Copy the MCP Toolkit URL and Token

2. Configure external clients (e.g., Claude Desktop, Cursor):
   ```json
   {
     "mcpServers": {
       "heroku-memory": {
         "url": "YOUR_MCP_TOOLKIT_URL",
         "headers": {
           "Authorization": "Bearer YOUR_MCP_TOOLKIT_TOKEN"
         }
       }
     }
   }
   ```

## Important Notes

### Memory Persistence

By default, the memory file is stored at `/app/memory.jsonl` on Heroku's ephemeral filesystem. This means:
- Memory is reset when the dyno restarts
- For persistent storage, consider:
  - Heroku Postgres with JSON storage
  - Heroku Redis for caching
  - External storage service

### Scaling

- MCP servers are automatically scaled to 0 when not in use
- They spin up on demand when requests come in
- Each tool call is limited to 300 seconds

### Cost

- You pay for dyno time when the MCP server is running
- Default eco dynos cost approximately $0.0008/second
- Inference usage is metered separately if using Heroku's models

## Environment Variables

The application uses these environment variables on Heroku:
- `PORT` - Automatically set by Heroku
- `QUARKUS_PROFILE=heroku` - Set via Procfile to use Heroku configuration

## Troubleshooting

### Container Image Build Error

If you see an error like:
```
Build step io.quarkus.container.image.jib.deployment.JibProcessor#buildFromJar threw an exception:
java.lang.IllegalStateException: No container runtime was found
```

This happens because the pom.xml includes the `quarkus-container-image-jib` dependency, but Heroku doesn't have Docker/Podman available during builds.

**Solution**: The `app.json` file already includes the necessary configuration to disable container image building:
```json
"MAVEN_CUSTOM_OPTS": {
  "description": "Custom Maven options for build",
  "value": "-DskipTests -Dquarkus.container-image.build=false"
}
```

If you still encounter issues, ensure your `app.json` contains this environment variable.

### Server Not Starting

Check logs:
```bash
heroku logs --tail -a your-mcp-memory-server
```

Common issues:
- Missing build artifacts - ensure you've built with the command above
- Wrong Java version - check `system.properties`

### MCP Tools Not Showing

- Ensure your Procfile has `mcp-` prefix for the process
- Check that the Managed Inference add-on is attached
- Verify in the Tools tab of the add-on dashboard

### Memory Not Persisting

This is expected behavior with ephemeral storage. Implement persistent storage solution if needed.

## Local Testing

Test the Heroku configuration locally:
```bash
# Build with Heroku profile
./mvnw clean package -Dquarkus.profile=heroku

# Run with PORT variable
PORT=8080 java -Dquarkus.profile=heroku -jar target/quarkus-app/quarkus-run.jar
```
