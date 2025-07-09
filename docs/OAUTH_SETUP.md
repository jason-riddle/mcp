# OAuth/OIDC Authentication Setup for MCP Server

This document describes how to set up OAuth/OIDC authentication for the MCP Server SSE endpoints.

## Overview

The MCP server now supports OAuth/OIDC authentication for SSE/HTTP transport, providing secure access to memory tools and preventing unauthorized usage. The implementation uses Quarkus OIDC extension and supports multiple OAuth providers.

## Features

- **Secure SSE Endpoints**: All MCP SSE endpoints require authentication
- **Tool-level Security**: Individual tools can be secured with role-based access
- **Multiple OAuth Providers**: Supports GitHub, Google, Auth0, and custom OIDC providers
- **Dev Mode Support**: Automatic Keycloak dev services for development
- **Production Ready**: Environment-based configuration for different OAuth providers

## Dependencies

The following dependencies have been added to support OAuth authentication:

```xml
<!-- Security Dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-oidc</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-qute</artifactId>
</dependency>
```

## Configuration

### Base Configuration

All MCP SSE endpoints are secured by default:

```properties
# Secure MCP SSE endpoints
quarkus.http.auth.permission.mcp-endpoints.paths=/mcp,/mcp/sse,/mcp/messages/*
quarkus.http.auth.permission.mcp-endpoints.policy=authenticated
```

### Development Mode

In development mode, Quarkus automatically provides Keycloak Dev Services with default users:
- Username: `alice`, Password: `alice`
- Username: `bob`, Password: `bob`

No additional configuration is needed for development.

### Production Configuration

#### GitHub OAuth

Add the following to `application-prod.properties`:

```properties
# GitHub OAuth configuration for production
%prod.quarkus.oidc.provider=github
%prod.quarkus.oidc.application-type=service
%prod.quarkus.oidc.client-id=${GITHUB_CLIENT_ID}
%prod.quarkus.oidc.credentials.secret=${GITHUB_CLIENT_SECRET}

# Login endpoint configuration (for obtaining tokens)
%prod.quarkus.oidc.login.provider=github
%prod.quarkus.oidc.login.client-id=${GITHUB_CLIENT_ID}
%prod.quarkus.oidc.login.credentials.secret=${GITHUB_CLIENT_SECRET}
```

Set the following environment variables:
- `GITHUB_CLIENT_ID`: Your GitHub OAuth app client ID
- `GITHUB_CLIENT_SECRET`: Your GitHub OAuth app client secret

#### Google OAuth

```properties
%prod.quarkus.oidc.provider=google
%prod.quarkus.oidc.client-id=${GOOGLE_CLIENT_ID}
%prod.quarkus.oidc.credentials.secret=${GOOGLE_CLIENT_SECRET}
```

#### Custom OIDC Provider

```properties
%prod.quarkus.oidc.auth-server-url=https://your-domain.auth0.com
%prod.quarkus.oidc.client-id=${AUTH0_CLIENT_ID}
%prod.quarkus.oidc.credentials.secret=${AUTH0_CLIENT_SECRET}
```

## GitHub OAuth App Setup

1. Go to GitHub Settings > Developer settings > OAuth Apps
2. Click "New OAuth App"
3. Set Application name: "MCP Memory Server"
4. Set Homepage URL: `http://localhost:8080` (or your production URL)
5. Set Authorization callback URL: `http://localhost:8080/login` (or your production URL)
6. Save the Client ID and Client Secret for environment variables

## Tool Security

### Authentication Requirements

All tools in `McpMemoryTools` now require authentication:

```java
@ApplicationScoped
@Authenticated  // All tools require authentication
public final class McpMemoryTools {
    // ...
}
```

### Role-Based Access Control

Admin-only operations use role-based access:

```java
@Tool(name = "memory_delete_entities", description = "Remove entities and their relations")
@RolesAllowed("admin")  // Only admin users can delete
public String deleteEntities(/* ... */) {
    // ...
}
```

### User Context Logging

Tools log user activity for audit purposes:

```java
@Tool(name = "memory_create_entities", description = "Create entities")
public String createEntities(List<Map<String, Object>> entities, McpLog log) {
    // ...
    String userName = identity.getPrincipal().getName();
    log.info("User %s created %d entities", userName, created.size());
    // ...
}
```

## Usage

### Obtaining Access Tokens

1. **Development Mode**:
   - Start the server: `./mvnw quarkus:dev`
   - Go to [Dev UI](http://localhost:8080/q/dev)
   - Use OIDC Dev UI to login and copy access tokens

2. **Production Mode**:
   - Start the server: `java -jar target/quarkus-app/quarkus-run.jar`
   - Navigate to `/login` endpoint
   - Complete OAuth flow
   - Copy the displayed access token

### Using Access Tokens

#### MCP Inspector

1. Install: `npx @modelcontextprotocol/inspector`
2. Set Transport Type: "SSE"
3. Set URL: `http://localhost:8080/mcp/sse`
4. Click "Authorization" and paste token in "Bearer Token" field
5. Connect and test tools

#### Curl

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \\
     -H "Content-Type: application/json" \\
     --data '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"memory_read_graph","arguments":{}}}' \\
     http://localhost:8080/mcp/messages/SESSION_ID
```

#### MCP Client Configuration

```json
{
  "mcpServers": {
    "memory-server": {
      "url": "http://localhost:8080/mcp/sse",
      "headers": {
        "Authorization": "Bearer YOUR_TOKEN"
      }
    }
  }
}
```

## Testing

### Integration Tests

The project includes secure integration tests that verify authentication:

- `SecureMcpServerSseIntegrationTest`: Tests authentication flows
- Basic auth setup for testing with users: `testuser`/`testpass` and `admin`/`adminpass`
- Role-based access control verification

### Running Tests

```bash
# Run all tests
./mvnw verify -DskipITs=false

# Run only secure integration tests
./mvnw test -Dtest=SecureMcpServerSseIntegrationTest
```

## Security Considerations

1. **Token Security**: Access tokens provide full access to MCP tools. Keep them secure and rotate regularly.

2. **HTTPS in Production**: Always use HTTPS in production to protect tokens in transit.

3. **Token Expiration**: Configure appropriate token expiration times based on your security requirements.

4. **Role Management**: Use OAuth provider's role/group features to manage admin access.

5. **Audit Logging**: All tool invocations are logged with user context for audit purposes.

## Troubleshooting

### Common Issues

1. **"401 Unauthorized"**: Check that your access token is valid and included in the Authorization header.

2. **"403 Forbidden"**: User may lack required roles for the requested operation.

3. **OAuth Redirect Issues**: Verify OAuth app callback URLs match your application URL.

4. **Token Display Issues**: Ensure GitHub OAuth app allows the callback URL for the login endpoint.

### Debug Configuration

Enable debug logging for authentication issues:

```properties
quarkus.log.category."io.quarkus.oidc".level=DEBUG
quarkus.log.category."io.quarkus.security".level=DEBUG
```

## Migration from Unsecured Setup

If migrating from an unsecured MCP server:

1. Update client configurations to include Authorization headers
2. Obtain access tokens through OAuth flow
3. Update any automation scripts to handle token refresh
4. Test all integrations with new authentication requirements

## Future Enhancements

- Multiple server configurations with different security requirements
- API key authentication option for simpler setups
- Integration with external identity providers
- Advanced role mapping and permissions
- Token refresh automation for long-running clients