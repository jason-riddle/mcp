# Remote MCP Server Configuration

This guide explains how to connect Claude Code, Claude.ai, and other clients to your remote Heroku MCP server.

## Table of Contents
- [Overview](#overview)
- [Server Information](#server-information)
- [Claude Code Configuration](#claude-code-configuration)
- [Claude.ai Web Interface Configuration](#claudeai-web-interface-configuration)
- [Other Clients](#other-clients)
- [Authentication](#authentication)
- [Troubleshooting](#troubleshooting)
- [Security Considerations](#security-considerations)

## Overview

Your Heroku MCP server provides 12 tools across three categories:
- **Memory Tools**: Persistent knowledge graph operations
- **Time Tools**: Timezone conversion and current time
- **Weather Tools**: Current weather, forecasts, and alerts

The server is accessible via Heroku's Managed Inference with OAuth authentication.

## Server Information

**Server URL**: `https://us.inference.heroku.com/mcp`
**Authentication**: Bearer Token (OAuth)
**Transport**: SSE (Server-Sent Events)
**Available Tools**: 12 tools (memory, time, weather)

## Claude Code Configuration

### Method 1: CLI Command (Recommended)

```bash
# Add the remote MCP server with authentication
claude mcp add --transport sse heroku-inf-mcp https://us.inference.heroku.com/mcp --header "Authorization: Bearer YOUR_TOKEN"
```

Replace `YOUR_TOKEN` with your actual Heroku MCP token from:
```bash
heroku config:get HEROKU_MCP_TOKEN -a jasons-mcp-server
```

### Method 2: Direct Configuration File

Create or edit `.claude.json` in your project root:

```json
{
  "mcpServers": {
    "heroku-inf-mcp": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "https://us.inference.heroku.com/mcp",
        "--header",
        "Authorization: Bearer ${HEROKU_MCP_TOKEN}"
      ],
      "env": {
        "HEROKU_MCP_TOKEN": "inf-REPLACE-WITH-INF-TOKEN"
      }
    }
  }
}
```

### Alternative Configuration Format

For better space handling:

```json
{
  "mcpServers": {
    "heroku-inf-mcp": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "https://us.inference.heroku.com/mcp",
        "--header",
        "Authorization:${AUTH_HEADER}"
      ],
      "env": {
        "AUTH_HEADER": "Bearer inf-REPLACE-WITH-INF-TOKEN"
      }
    }
  }
}
```

### Managing Configurations

```bash
# List all configured servers
claude mcp list

# Get details for your server
claude mcp get heroku-inf-mcp

# Remove server if needed
claude mcp remove heroku-inf-mcp

# Check status in interactive mode
/mcp
```

## Claude.ai Web Interface Configuration

**Requirements**: Claude Pro, Max, Team, or Enterprise plan

### Setup Steps

1. **Access Settings**
   - Go to [claude.ai](https://claude.ai)
   - Click your profile icon → "Settings"
   - Navigate to "Integrations" section

2. **Add Custom Integration**
   - Click "Add Custom Integration"
   - Select "Remote MCP Server"

3. **Configuration**
   ```json
   {
     "type": "url",
     "url": "https://us.inference.heroku.com/mcp",
     "name": "heroku-inf-mcp",
     "authorization_token": "inf-REPLACE-WITH-INF-TOKEN"
   }
   ```

4. **Authentication**
   - Click "Connect" to authenticate
   - Grant necessary permissions
   - Enable/disable specific tools as needed

## Other Clients

### Cursor IDE

Configure in Tools & Integrations settings:

```json
{
  "mcpServers": {
    "heroku-inf-mcp": {
      "url": "https://us.inference.heroku.com/mcp",
      "headers": {
        "Authorization": "Bearer inf-REPLACE-WITH-INF-TOKEN"
      }
    }
  }
}
```

### Generic HTTP Client

```bash
# Test tools endpoint
curl -H "Authorization: Bearer inf-REPLACE-WITH-INF-TOKEN" \
     https://us.inference.heroku.com/mcp/tools

# Test SSE endpoint
curl -H "Authorization: Bearer inf-REPLACE-WITH-INF-TOKEN" \
     https://us.inference.heroku.com/mcp/sse
```

## Authentication

### Getting Your Token

Your Heroku MCP token is available as an environment variable:

```bash
# View current token
heroku config:get HEROKU_MCP_TOKEN -a jasons-mcp-server

# Current token: inf-REPLACE-WITH-INF-TOKEN
```

### OAuth Flow (Web Interface)

For Claude.ai web interface:
1. Authentication uses OAuth 2.0
2. Click "Connect" during setup
3. Complete OAuth flow in browser
4. Permissions are managed in Claude settings

### Token Security

- **Store tokens securely**: Use environment variables
- **Limit scope**: Tokens are specific to your Heroku MCP server
- **Rotation**: Tokens are managed by Heroku Managed Inference
- **Revocation**: Remove integrations from Claude settings to revoke

## Troubleshooting

### Connection Issues

**Symptom**: Connection hangs or continuously retries
**Solution**:
1. Clear browser cookies for `us.inference.heroku.com`
2. Remove MCP server configuration
3. Restart client
4. Re-add server configuration
5. Re-authenticate

### Authentication Failures

**Symptom**: 401 Unauthorized errors
**Solution**:
1. Verify token is correct:
   ```bash
   heroku config:get HEROKU_MCP_TOKEN -a jasons-mcp-server
   ```
2. Check token format includes `inf-` prefix
3. Ensure Bearer token format: `Bearer inf-...`

### Tool Discovery Issues

**Symptom**: No tools appear or incomplete tool list
**Solution**:
1. Test server directly:
   ```bash
   make test-e2e
   ```
2. Verify all 12 tools are available
3. Check client logs for errors

### Server Status

```bash
# Check Heroku app status
heroku ps -a jasons-mcp-server

# View recent logs
heroku logs --tail -a jasons-mcp-server

# Test functionality
make test-e2e
```

## Security Considerations

### Best Practices

- **Trust Verification**: Only connect to trusted MCP servers
- **Token Management**: Store tokens securely, avoid hardcoding
- **Permission Review**: Regularly review granted permissions
- **Network Security**: Use HTTPS endpoints only

### Risk Mitigation

- **Prompt Injection**: Be cautious with internet-connected tools
- **Data Privacy**: Review what data is shared with the server
- **Access Control**: Use least-privilege principles
- **Monitoring**: Monitor usage and access patterns

### Revoking Access

**Claude.ai Web Interface**:
- Settings → Integrations → Disconnect

**Claude Code**:
```bash
claude mcp remove heroku-inf-mcp
```

**Heroku Level** (if needed):
- Regenerate HEROKU_MCP_TOKEN via Heroku support

## Available Tools

Your Heroku MCP server provides these tools:

### Memory Tools
- `memory.create_entities` - Create knowledge graph entities
- `memory.create_relations` - Create entity relationships
- `memory.add_observations` - Add observations to entities
- `memory.read_graph` - Read entire knowledge graph
- `memory.search_nodes` - Search for specific nodes
- `memory.open_nodes` - Retrieve specific nodes by name
- `memory.delete_entities` - Remove entities and relations
- `memory.delete_observations` - Remove specific observations
- `memory.delete_relations` - Remove specific relations

### Time Tools
- `time.get_current_time` - Get current time in timezone
- `time.convert_time` - Convert between timezones

### Weather Tools
- `weather.current` - Current weather for location
- `weather.forecast` - Weather forecast for location
- `weather.alerts` - Weather alerts for location

## Support

For issues with this MCP server:
1. Check the [troubleshooting section](#troubleshooting)
2. Review Heroku app logs
3. Test with `make test-e2e`
4. File issues in the project repository
