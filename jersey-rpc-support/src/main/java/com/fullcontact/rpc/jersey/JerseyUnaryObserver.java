package com.fullcontact.rpc.jersey;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;

import javax.ws.rs.container.AsyncResponse;

/**
 * gRPC StreamObserver which publishes to a Jersey AsyncResponse. Used for unary (singular request/response)
 * semantics.
 */
public class JerseyUnaryObserver<V extends Message> implements StreamObserver<V> {
    private final AsyncResponse asyncResponse;

    private volatile boolean closed = false;

    public JerseyUnaryObserver(AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
    }

    @Override
    public void onNext(V value) {
        if(closed)
            throw new IllegalStateException("JerseyUnaryObserver has already been closed");
        try {
            asyncResponse.resume(JsonHandler.unaryPrinter().print(value));
        }
        catch(InvalidProtocolBufferException e) {
            onError(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        closed = true;
        ErrorHandler.handleUnaryError(t, asyncResponse);
    }

    @Override
    public void onCompleted() {
        closed = true;
    }
}
