package com.fullcontact.rpc.jersey;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

/**
 * gRPC StreamObserver which publishes to a Jersey AsyncResponse
 *
 * Currently does not handle multiple (streaming) messages
 *
 * @author Michael Rose (xorlev)
 */
public class JerseyUnaryObserver<V extends Message> implements StreamObserver<V> {
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer().includingDefaultValueFields();
    private final AsyncResponse asyncResponse;

    private volatile boolean closed = false;

    public JerseyUnaryObserver(AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
    }

    @Override
    public void onNext(V value) {
        if(closed)
            throw new IllegalStateException("StreamingObserver has already been closed");
        // TODO, content-negotiated handler
        try {
            asyncResponse.resume(PRINTER.print(value));
        }
        catch(InvalidProtocolBufferException e) {
            onError(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        closed = true;
        if(t instanceof InvalidProtocolBufferException) {
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).entity(t.getMessage()).build());
        } else {
            asyncResponse.resume(GrpcErrorUtil.createJerseyResponse(t));
        }
    }

    @Override
    public void onCompleted() {
        closed = true;
    }
}
