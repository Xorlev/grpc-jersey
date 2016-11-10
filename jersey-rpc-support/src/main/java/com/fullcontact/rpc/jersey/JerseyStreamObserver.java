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
public class JerseyStreamObserver<V extends Message> implements StreamObserver<V> {
    private final AsyncResponse asyncResponse;

    public JerseyStreamObserver(AsyncResponse response) {
        asyncResponse = response;
    }

    @Override
    public void onNext(V value) {
        // TODO, content-negotiated handler
        try {
            asyncResponse.resume(JsonFormat.printer().print(value));
        }
        catch(InvalidProtocolBufferException e) {
            onError(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        if(t instanceof InvalidProtocolBufferException) {
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).entity(t.getMessage()).build());
        } else {
            asyncResponse.resume(GrpcErrorUtil.createJerseyResponse(t));
        }
    }

    @Override
    public void onCompleted() {

    }
}
