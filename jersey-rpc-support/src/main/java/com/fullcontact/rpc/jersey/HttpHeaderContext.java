package com.fullcontact.rpc.jersey;

import com.google.common.annotations.Beta;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import io.grpc.Context;
import java.util.Collection;
import java.util.Map;

/**
 * Utilities for retrieving and manipulating HTTP headers from a gRPC context.
 *
 * <p>Works by attaching an interceptor which bundles headers as a side-channel to the RPC and unbundles headers from
 * the RPC responses side-channel. A server interceptor handles muxing and demuxing the headers into the Context.
 *
 * While `HttpHeaderContext` is gRPC {@link Context} aware and request headers can be safely accessed from background
 * threads executed with an attached context, manipulating response headers should only be done from a single thread as
 * no effort is put into synchronizing the state.
 */
@Beta
public class HttpHeaderContext {
    static final Context.Key<Multimap<String, String>> REQUEST_HEADERS = Context
            .keyWithDefault("grpc-jersey-request-headers", HashMultimap.create());
    static final Context.Key<Multimap<String, String>> RESPONSE_HEADERS = Context
            .keyWithDefault("grpc-jersey-response-headers", HashMultimap.create());

    private HttpHeaderContext() {} // Do not instantiate.

    /**
     * Adds a header to the final list of output headers. Does not clear headers with existing name.
     *
     * Not thread-safe.
     */
    public static void addResponseHeader(String name, String value) {
        RESPONSE_HEADERS.get().put(name, value);
    }

    /**
     * Adds a header to the final list of output headers. Clear headers with existing name before adding.
     *
     * Not thread-safe.
     */
    public static void setResponseHeader(String name, String value) {
        clearResponseHeader(name);
        addResponseHeader(name, value);
    }

    /**
     * Adds a header to the final list of output headers. Clear headers with existing name before adding.
     *
     * Not thread-safe.
     */
    public static void setResponseHeader(String name, Collection<String> value) {
        clearResponseHeader(name);
        RESPONSE_HEADERS.get().putAll(name, value);
    }

    /**
     * Removes a header from the set of response headers.
     *
     * Not thread-safe.
     */
    public static void clearResponseHeader(String name) {
        RESPONSE_HEADERS.get().removeAll(name);
    }

    /**
     * Removes all in-progress response headers.
     *
     * Not thread-safe.
     */
    public static void clearResponseHeaders() {
        RESPONSE_HEADERS.get().clear();
    }

    /**
     * Returns an immutable copy of the request headers, if any.
     */
    public static ImmutableMultimap<String, String> requestHeaders() {
        return ImmutableMultimap.copyOf(REQUEST_HEADERS.get());
    }

    /**
     * Returns a immutable copy of the request headers, taking the first value of each header if there are multiple.
     */
    public static ImmutableMap<String, String> requestHeadersFirstValue() {
        return firstValueFromEachKey(REQUEST_HEADERS.get());
    }

    /**
     * Returns a immutable copy of the response headers.
     */
    public static ImmutableMultimap<String, String> responseHeaders() {
        return ImmutableMultimap.copyOf(RESPONSE_HEADERS.get());
    }

    /**
     * Returns a immutable copy of the response headers, taking the first value of each header if there are multiple.
     */
    public static ImmutableMap<String, String> responseHeadersFirstValue() {
        return firstValueFromEachKey(RESPONSE_HEADERS.get());
    }

    private static ImmutableMap<String, String> firstValueFromEachKey(Multimap<String, String> multimap) {
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();

        for (Map.Entry<String, Collection<String>> entry : multimap.asMap().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                map.put(entry.getKey(), Iterables.getFirst(entry.getValue(), null));
            }
        }

        return map.build();
    }
}
