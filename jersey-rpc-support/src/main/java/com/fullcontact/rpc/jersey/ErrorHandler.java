package com.fullcontact.rpc.jersey;

import javax.ws.rs.container.AsyncResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Registry for (currently) JVM-global gRPC error handlers. This allows users to override the error handling
 * methodology without patching the library.
 *
 * This class should be considered unstable, a more comprehensive plugin mechanism will be built in the future.
 */
public final class ErrorHandler {
    private static GrpcJerseyErrorHandler errorHandler = new GrpcJerseyErrorHandler.Default();

    private ErrorHandler() {}

    static void handleUnaryError(Throwable t, AsyncResponse response) {
        errorHandler.handleUnaryError(t, response);
    }

    static Optional<String> handleStreamingError(Throwable t) throws IOException {
        return errorHandler.handleStreamingError(t);
    }

    /**
     * Overrides the default error handler on a global basis. Beware, this shouldn't be done after requests start.
     */
    public static void setErrorHandler(GrpcJerseyErrorHandler errorHandler) {
        ErrorHandler.errorHandler = errorHandler;
    }
}
