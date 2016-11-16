package com.fullcontact.rpc.jersey;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Test support class, exposes a Map of strings as UriInfo query params, wrapping the map as a MultivaluedMap (Jersey)
 *
 * @author Michael Rose (xorlev)
 */
public class HttpHeadersMap implements HttpHeaders {
    private final MultivaluedHashMap<String, String> map;

    public HttpHeadersMap() {
        this.map = new MultivaluedHashMap<>();
    }

    public HttpHeadersMap(Map<String, String> map) {
        this.map = new MultivaluedHashMap<>(map);
    }

    public HttpHeadersMap put(String key, String value) {
        map.add(key, value);

        return this;
    }

    @Override
    public List<String> getRequestHeader(String name) {
        return map.get(name);
    }

    @Override
    public String getHeaderString(String name) {
        return map.getFirst(name);
    }

    @Override
    public MultivaluedMap<String, String> getRequestHeaders() {
        return map;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        throw new UnsupportedOperationException("HttpHeadersMap hasn't implemented this method");
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        throw new UnsupportedOperationException("HttpHeadersMap hasn't implemented this method");
    }

    @Override
    public MediaType getMediaType() {
        throw new UnsupportedOperationException("HttpHeadersMap hasn't implemented this method");
    }

    @Override
    public Locale getLanguage() {
        throw new UnsupportedOperationException("HttpHeadersMap hasn't implemented this method");
    }

    @Override
    public Map<String, Cookie> getCookies() {
        throw new UnsupportedOperationException("HttpHeadersMap hasn't implemented this method");
    }

    @Override
    public Date getDate() {
        throw new UnsupportedOperationException("HttpHeadersMap hasn't implemented this method");
    }

    @Override
    public int getLength() {
        throw new UnsupportedOperationException("HttpHeadersMap hasn't implemented this method");
    }
}
