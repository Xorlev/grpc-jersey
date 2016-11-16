package com.fullcontact.rpc.jersey;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Test support class, exposes a Map of strings as UriInfo query params, wrapping the map as a MultivaluedMap (Jersey)
 *
 * @author Michael Rose (xorlev)
 */
public class UriInfoMap implements UriInfo {
    private final MultivaluedHashMap<String, String> map;

    public UriInfoMap() {
        this.map = new MultivaluedHashMap<>();
    }

    public UriInfoMap(Map<String, String> map) {
        this.map = new MultivaluedHashMap<>(map);
    }

    public UriInfoMap put(String key, String value) {
        map.add(key, value);

        return this;
    }

    @Override
    public String getPath() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public String getPath(boolean decode) {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public List<PathSegment> getPathSegments() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public URI getRequestUri() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public URI getAbsolutePath() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public URI getBaseUri() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return map;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return map;
    }

    @Override
    public List<String> getMatchedURIs() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public List<Object> getMatchedResources() {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public URI resolve(URI uri) {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }

    @Override
    public URI relativize(URI uri) {
        throw new UnsupportedOperationException("UriInfoMap hasn't implemented this method");
    }
}
