package com.fullcontact.rpc.jersey;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Pluggable error handler used by the {@link JerseyUnaryObserver} and {@link JerseyStreamingObserver}.
 */
@Beta
public interface GrpcJerseyErrorHandler {
    /**
     * Handles an exception raised in a unary (request/response) RPC handler.
     *
     * It is up to each implementation as to whether they honor the responseHeaders set by the RPC handler.
     *
     * @param t throwable raised
     * @param responseHeaders headers set by the RPC handler
     * @return response JAX-RS Response. Returning {@link Optional#empty()} will call {@link AsyncResponse#cancel()}.
     */
    Optional<Response> handleUnaryError(Throwable t, ImmutableMultimap<String, String> responseHeaders);

    /**
     * Handles an exception raised in a server streaming RPC handler. As HTTP/1.1 practically doesn't support trailers,
     * there isn't a real way to signal well-formed errors except via another streaming payload.
     *
     * @param t throwable raised.
     * @return Literal string, if you want JSON-encoded data use the {@link JsonHandler#streamPrinter()} to
     *         retain server-sent events compatibility. Return {@link Optional#empty()} to silently abort.
     * @throws IOException usually if serialization of errors break.
     */
    Optional<String> handleStreamingError(Throwable t) throws IOException;

    class Default implements GrpcJerseyErrorHandler {
        @Override
        public Optional<Response> handleUnaryError(Throwable t, ImmutableMultimap<String, String> responseHeaders) {
            Response response;
            if (t instanceof InvalidProtocolBufferException) {
                response = Response.status(Response.Status.BAD_REQUEST).entity(t.getMessage()).build();
            } else {
                response = GrpcErrorUtil.createJerseyResponse(t);
            }

            if (!responseHeaders.isEmpty()) {
                ResponseBuilder responseBuilder = Response.fromResponse(response);
                for (Map.Entry<String, String> entry : responseHeaders.entries()) {
                    responseBuilder.header(entry.getKey(), entry.getValue());
                }

                response = responseBuilder.build();
            }

            return Optional.of(response);
        }

        @Override
        public Optional<String> handleStreamingError(Throwable t) throws InvalidProtocolBufferException {
            Status grpcError = GrpcErrorUtil.throwableToStatus(t).getPayload();

            // JsonFormat doesn't support serializing Any.
            if (!grpcError.getDetailsList().isEmpty()) {
                grpcError = grpcError.toBuilder().clearDetails().build();
            }

            return Optional.of(JsonHandler.streamPrinter().print(grpcError));
        }
    }
}
