package com.fullcontact.rpc.jersey;

import io.grpc.*;

/**
 * Common set of interceptors and mutations done to stubs + channels. Users who opt into using this utility class to
 * register common interceptors will automatically have new platform features when available.
 *
 * If your service depends on HTTP headers in the Context for further interceptors, make sure to apply this set of
 * interceptors before your own.
 */
public class GrpcJerseyPlatformInterceptors {
    private GrpcJerseyPlatformInterceptors() {} // Do not instantiate.

    public static ServerServiceDefinition intercept(BindableService bindableService) {
        return intercept(bindableService.bindService());
    }

    public static ServerServiceDefinition intercept(ServerServiceDefinition serverServiceDefinition) {
        return ServerInterceptors.intercept(serverServiceDefinition, HttpHeaderInterceptors.serverInterceptor());
    }
}
