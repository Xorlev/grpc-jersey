package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.jersey.util.JsonUtil;
import com.google.protobuf.InvalidProtocolBufferException;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;

/**
 * Pluggable error handler used by the {@link JerseyUnaryObserver} and {@link JerseyStreamingObserver}.
 */
public interface GrpcJerseyErrorHandler {
    /**
     * Handles an exception raised in a unary (request/response) RPC handler.
     *
     * @param t throwable raised
     * @param response JAX-RS AsyncResponse, can call cancel() or resume() with a string payload or {@link Response}.
     */
    void handleUnaryError(Throwable t, AsyncResponse response);

    /**
     * Handles an exception raised in a server streaming RPC handler. As HTTP/1.1 practically doesn't support trailers,
     * there isn't a real way to signal well-formed errors except via another streaming payload.
     *
     * @param t throwable raised.
     * @return Literal string, if you want JSON-encoded data use the {@link JsonUtil#PRINTER_WITHOUT_WHITESPACE} to
     *         retain server-sent events compatibility. Return {@link Optional#empty()} to silently abort.
     * @throws InvalidProtocolBufferException
     */
    Optional<String> handleStreamingError(Throwable t) throws IOException;

    class Default implements GrpcJerseyErrorHandler {

        @Override
        public void handleUnaryError(Throwable t, AsyncResponse asyncResponse) {
            if(t instanceof InvalidProtocolBufferException) {
                asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).entity(t.getMessage()).build());
            } else {
                asyncResponse.resume(GrpcErrorUtil.createJerseyResponse(t));
            }
        }

        @Override
        public Optional<String> handleStreamingError(Throwable t) throws InvalidProtocolBufferException {
            return Optional.of(
                    JsonUtil.PRINTER_WITHOUT_WHITESPACE.print(GrpcErrorUtil.throwableToStatus(t).getPayload()));
        }
    }
}
