package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.jersey.HttpHeaderInterceptors.HttpHeaderClientInterceptor;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

/**
 * gRPC StreamObserver which publishes JSON-formatted messages from a gRPC server stream. Uses underlying servlet.
 * {@link AsyncContext}.
 *
 * @author Michael Rose (xorlev)
 */
public class JerseyStreamingObserver<V extends Message> implements StreamObserver<V> {
    public static final List<Variant> VARIANT_LIST = ImmutableList.of(
            new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, null),
            new Variant(new MediaType("text", "event-stream"), (String) null, null)
    );

    private final AsyncContext asyncContext;
    private final HttpHeaderClientInterceptor httpHeaderClientInterceptor;
    private final HttpServletResponse httpServletResponse;
    private final ServletOutputStream outputStream;
    private final boolean sse;

    private volatile boolean first = true;
    private volatile boolean closed = false;

    // Reusable buffer used in the context of a single streaming request, starts at 128 bytes.
    private StringBuilder buffer = new StringBuilder(128);

    public JerseyStreamingObserver(
            HttpHeaderClientInterceptor httpHeaderClientInterceptor,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            boolean sse)
            throws IOException {
        this.asyncContext = httpServletRequest.getAsyncContext();
        this.httpHeaderClientInterceptor = httpHeaderClientInterceptor;
        this.httpServletResponse = httpServletResponse;
        this.outputStream = asyncContext.getResponse().getOutputStream();
        this.sse = sse;
    }

    @Override
    public void onNext(V value) {
        if (closed) {
            throw new IllegalStateException("JerseyStreamingObserver has already been closed");
        }

        addHeadersIfNotSent();

        try {
            write(JsonHandler.streamPrinter().print(value));
        } catch (IOException e) {
            onError(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        if (t instanceof EOFException) {
            closed = true;
            // The client went away, there's not much we can do.
            return;
        }

        try {
            // Send headers if we haven't sent anything yet.
            addHeadersIfNotSent();

            // As we lack supported trailers in standard HTTP, we'll have to make do with emitting an error to the
            // primary stream
            Optional<String> errorPayload = ErrorHandler.handleStreamingError(t);
            if (errorPayload.isPresent()) {
                write(errorPayload.get());
            }

            closed = true;
            outputStream.close();
            asyncContext.complete();
        } catch (IOException e) {
            // Something really broke, try closing the connection.
            try {
                outputStream.close();
                asyncContext.complete();
            } catch (IOException e1) {
                // Ignored if we already have.
            }
        }
    }

    @Override
    public void onCompleted() {
        addHeadersIfNotSent();

        try {
            closed = true;
            outputStream.flush();
            outputStream.close();
            asyncContext.complete();
        } catch (IOException e) {
            onError(e);
        }
    }

    private void addHeadersIfNotSent() {
        if (!first || closed) {
            return;
        } else {
            first = false;
        }

        for (Map.Entry<String, String> header : httpHeaderClientInterceptor.getHttpResponseHeaders().entries()) {
            httpServletResponse.addHeader(header.getKey(), header.getValue());
        }
    }

    private void write(String value) throws IOException {
        if (value.isEmpty()) {
            return;
        }

        if (sse) {
            buffer.append("data: ");
        }

        buffer.append(value).append('\n');

        if (sse) {
            buffer.append('\n');
        }

        outputStream.print(buffer.toString());
        outputStream.flush();

        // Reset buffer position to 0. At this point, the buffer will have a capacity of the max size(value) passed
        // through so far. In the majority of cases, other messages will be of similar (or larger) size,
        // so despite the fact that we might be holding onto a multi-mb buffer, it avoids the need continually
        // allocate new buffers per message
        buffer.setLength(0);
    }
}
