package com.fullcontact.rpc.jersey;

import static com.fullcontact.rpc.jersey.HttpHeaderContext.REQUEST_HEADERS;
import static com.fullcontact.rpc.jersey.HttpHeaderContext.RESPONSE_HEADERS;

import com.fullcontact.rpc.Header;
import com.fullcontact.rpc.Headers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class HttpHeaderInterceptors {
    private static final Metadata.Key<Headers> HEADERS_KEY = ProtoUtils.keyForProto(Headers.getDefaultInstance());

    private HttpHeaderInterceptors() {} // Do not instantiate.

    /**
     * Returns the server interceptor necessary to make {@link HttpHeaderContext} work. It is recommended you use {@link
     * GrpcJerseyPlatformInterceptors#intercept} if possible.
     */
    public static HttpHeaderServerInterceptor serverInterceptor() {
        return HttpHeaderServerInterceptor.INSTANCE;
    }

    /**
     * Returns the client interceptor used to extract the HTTP headers from the RPC sidechannel. Public for use in
     * generated code, should not be used by the end user.
     */
    public static HttpHeaderClientInterceptor clientInterceptor(HttpHeaders httpHeaders) {
        return new HttpHeaderClientInterceptor(httpHeaders);
    }

    private static Headers headersFromMultimap(Multimap<String, String> headers) {
        Headers.Builder builder = Headers.newBuilder();

        if (headers == null) {
            return builder.build();
        }

        for (Map.Entry<String, String> header : headers.entries()) {
            builder.addHeader(Header.newBuilder()
                    .setName(header.getKey())
                    .setValue(header.getValue())
                    .build());
        }

        return builder.build();
    }

    private static ImmutableMultimap<String, String> toMultimapFromHeaders(Headers headers) {
        if (headers == null) {
            return ImmutableMultimap.of();
        }

        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        for (Header header : headers.getHeaderList()) {
            builder.put(header.getName(), header.getValue());
        }

        return builder.build();
    }

    private static ImmutableMultimap<String, String> toMultimapFromJerseyHeaders(HttpHeaders headers) {
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        for (Map.Entry<String, List<String>> header : headers.getRequestHeaders().entrySet()) {
            builder.putAll(header.getKey(), header.getValue());
        }

        return builder.build();
    }

    public static class HttpHeaderClientInterceptor implements ClientInterceptor {
        private final ImmutableMultimap<String, String> httpRequestHeaders;

        private ImmutableMultimap<String, String> httpResponseHeaders = ImmutableMultimap.of();
        private boolean receivedHeaders = false;

        HttpHeaderClientInterceptor(HttpHeaders httpRequestHeaders) {
            this.httpRequestHeaders = toMultimapFromJerseyHeaders(httpRequestHeaders);
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    // Bundle known request headers into RPC side-channel.
                    headers.put(HEADERS_KEY, headersFromMultimap(httpRequestHeaders));

                    delegate().start(
                            new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                                    responseListener) {
                                @Override
                                public void onHeaders(Metadata headers) {
                                    processMetadata(headers);
                                    super.onHeaders(headers);
                                }

                                @Override
                                public void onClose(Status status, Metadata trailers) {
                                    processMetadata(trailers);
                                    super.onClose(status, trailers);
                                }

                                private void processMetadata(Metadata metadata) {
                                    if (receivedHeaders) {
                                        return;
                                    }

                                    // Set response headers if present on RPC.
                                    if (metadata.containsKey(HEADERS_KEY)) {
                                        receivedHeaders = true;
                                        httpResponseHeaders = toMultimapFromHeaders(metadata.get(HEADERS_KEY));
                                    }
                                }
                            }, headers);
                }
            };
        }

        ImmutableMultimap<String, String> getHttpResponseHeaders() {
            return httpResponseHeaders;
        }

        Response.ResponseBuilder withResponseHeaders(Response.ResponseBuilder builder) {
            if (!httpResponseHeaders.isEmpty()) {
                for (Map.Entry<String, String> header : httpResponseHeaders.entries()) {
                    builder.header(header.getKey(), header.getValue());
                }
            }

            return builder;
        }
    }

    private static class HttpHeaderServerInterceptor implements ServerInterceptor {
        private static final HttpHeaderServerInterceptor INSTANCE = new HttpHeaderServerInterceptor();

        private HttpHeaderServerInterceptor() {}

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            Context context = Context.current()
                    .withValues(
                            REQUEST_HEADERS, toMultimapFromHeaders(headers.get(HEADERS_KEY)),
                            RESPONSE_HEADERS, HashMultimap.create());

            boolean sideChannelOn = headers.containsKey(HEADERS_KEY);
            ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> simpleForwardingServerCall =
                    new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                        private boolean sentHeaders = false;

                        @Override
                        public void sendHeaders(Metadata headers) {
                            if(sideChannelOn) {
                                headers.put(HEADERS_KEY, headersFromMultimap(RESPONSE_HEADERS.get()));
                            }
                            sentHeaders = true;
                            super.sendHeaders(headers);
                        }

                        @Override
                        public void close(Status status, Metadata trailers) {
                            if (!sentHeaders && sideChannelOn) {
                                trailers.put(HEADERS_KEY, headersFromMultimap(RESPONSE_HEADERS.get()));
                            }

                            super.close(status, trailers);
                        }
                    };

            return Contexts.interceptCall(context, simpleForwardingServerCall, headers, next);
        }
    }
}
