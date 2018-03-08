package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.jersey.HttpHeaderInterceptors.HttpHeaderClientInterceptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import java.util.Optional;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

/**
 * gRPC StreamObserver which publishes to a Jersey AsyncResponse. Used for unary (singular request/response) semantics.
 */
public class JerseyUnaryObserver<V extends Message> implements StreamObserver<V> {
    private final AsyncResponse asyncResponse;
    private final HttpHeaderClientInterceptor httpHeaderClientInterceptor;

    private volatile boolean closed = false;

    public JerseyUnaryObserver(AsyncResponse asyncResponse, HttpHeaderClientInterceptor httpHeaderClientInterceptor) {
        this.asyncResponse = asyncResponse;
        this.httpHeaderClientInterceptor = httpHeaderClientInterceptor;
    }

    @Override
    public void onNext(V value) {
        if (closed) {
            throw new IllegalStateException("JerseyUnaryObserver has already been closed");
        }
        try {
            Response response = httpHeaderClientInterceptor
                    .withResponseHeaders(Response.ok())
                    .entity(JsonHandler.unaryPrinter().print(value))
                    .build();
            asyncResponse.resume(response);
            closed = true;
        } catch (InvalidProtocolBufferException e) {
            onError(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        closed = true;
        Optional<Response> response = ErrorHandler
                .handleUnaryError(t, httpHeaderClientInterceptor.getHttpResponseHeaders());
        if (response.isPresent()) {
            asyncResponse.resume(response.get());
        } else {
            asyncResponse.cancel();
        }
    }

    @Override
    public void onCompleted() {
        closed = true;
    }
}
