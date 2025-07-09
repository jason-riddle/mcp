package com.jasonriddle.mcp.auth;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.UserInfo;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Login endpoint for OAuth flow to obtain MCP access tokens.
 */
@Path("/login")
@Authenticated
public class LoginResource {

    @Inject
    UserInfo userInfo;

    @Inject
    AccessTokenCredential accessToken;

    @Inject
    SecurityIdentity identity;

    @Inject
    Template tokenDisplay;

    /**
     * Displays the access token after successful OAuth login.
     *
     * @return HTML page with access token.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getToken() {
        return tokenDisplay
                .data("userName", userInfo.getName())
                .data("userLogin", identity.getPrincipal().getName())
                .data("accessToken", accessToken.getToken())
                .data("userRoles", identity.getRoles());
    }
}