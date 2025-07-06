package com.jasonriddle.mcp.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Test implementation of ContainerRequestContext for unit testing.
 */
final class TestContainerRequestContext implements ContainerRequestContext {

    private final Map<String, String> headers = new HashMap<>();
    private boolean aborted = false;
    private Response response;

    /**
     * Set a header value for testing.
     *
     * @param headerName the header name
     * @param headerValue the header value
     */
    public void setHeaderString(final String headerName, final String headerValue) {
        if (headerValue == null) {
            headers.remove(headerName);
        } else {
            headers.put(headerName, headerValue);
        }
    }

    /**
     * Check if the request was aborted.
     *
     * @return true if the request was aborted
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * Get the response used to abort the request.
     *
     * @return the response used to abort the request
     */
    public Response getResponse() {
        return response;
    }

    @Override
    public Object getProperty(final String name) {
        return null;
    }

    @Override
    public Collection<String> getPropertyNames() {
        return List.of();
    }

    @Override
    public void setProperty(final String name, final Object object) {
        // Not implemented for test
    }

    @Override
    public void removeProperty(final String name) {
        // Not implemented for test
    }

    @Override
    public UriInfo getUriInfo() {
        return null;
    }

    @Override
    public void setRequestUri(final URI requestUri) {
        // Not implemented for test
    }

    @Override
    public void setRequestUri(final URI baseUri, final URI requestUri) {
        // Not implemented for test
    }

    @Override
    public Request getRequest() {
        return null;
    }

    @Override
    public String getMethod() {
        return null;
    }

    @Override
    public void setMethod(final String method) {
        // Not implemented for test
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return null;
    }

    @Override
    public String getHeaderString(final String name) {
        return headers.get(name);
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public MediaType getMediaType() {
        return null;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return List.of();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return List.of();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return Map.of();
    }

    @Override
    public boolean hasEntity() {
        return false;
    }

    @Override
    public InputStream getEntityStream() {
        return null;
    }

    @Override
    public void setEntityStream(final InputStream input) {
        // Not implemented for test
    }

    @Override
    public SecurityContext getSecurityContext() {
        return null;
    }

    @Override
    public void setSecurityContext(final SecurityContext context) {
        // Not implemented for test
    }

    @Override
    public void abortWith(final Response abortResponse) {
        this.aborted = true;
        this.response = abortResponse;
    }
}
