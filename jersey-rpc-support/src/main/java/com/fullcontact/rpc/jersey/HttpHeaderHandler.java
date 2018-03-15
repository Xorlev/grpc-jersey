package com.fullcontact.rpc.jersey;

import com.google.common.collect.ImmutableSet;

/**
 * Global settings for passing behavior of grpc response headers.
 * <p>
 * <p>This class should be considered unstable, a more comprehensive plugin mechanism will be built in the future.
 *
 * @author smartwjw
 * @since 2018-03-15
 */
public final class HttpHeaderHandler {

    private static boolean includeGrpcResponseHeaders = true;
    private static ImmutableSet<String> blackListedHeaders = ImmutableSet.of();

    private HttpHeaderHandler() {
    }

    public static void setIncludeGrpcResponseHeaders(boolean includeGrpcResponseHeaders) {
        HttpHeaderHandler.includeGrpcResponseHeaders = includeGrpcResponseHeaders;
    }

    public static void setBlackListedHeaders(Iterable<String> blackListedHeaders) {
        HttpHeaderHandler.blackListedHeaders = ImmutableSet.copyOf(blackListedHeaders);
    }

    public static boolean isIncludeGrpcResponseHeaders() {
        return includeGrpcResponseHeaders;
    }

    public static ImmutableSet<String> getBlackListedHeaders() {
        return blackListedHeaders;
    }
}
