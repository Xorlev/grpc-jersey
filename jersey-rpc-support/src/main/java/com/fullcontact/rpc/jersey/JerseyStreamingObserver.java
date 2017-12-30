package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.jersey.util.JsonUtil;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.glassfish.jersey.server.ChunkedOutput;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * gRPC StreamObserver which publishes to a Jersey ChunkedOutput. Used for server-side streaming of messages.
 *
 * @author Michael Rose (xorlev)
 */
public class JerseyStreamingObserver<V extends Message> implements StreamObserver<V> {
    public static final List<Variant> VARIANT_LIST = ImmutableList.of(
        new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, null),
        new Variant(new MediaType("text", "event-stream"), (String) null, null)
    );

    private final ChunkedOutput<String> output;
    private final boolean sse;

    private volatile boolean closed = false;

    // Reusable buffer used in the context of a single streaming request, starts at 128 bytes.
    private StringBuilder buffer = new StringBuilder(128);

    public JerseyStreamingObserver(ChunkedOutput<String> output, boolean sse) {
        this.output = output;
        this.sse = sse;
    }

    @Override
    public void onNext(V value) {
        if(closed)
            throw new IllegalStateException("JerseyStreamingObserver has already been closed");

        try {
            write(JsonUtil.PRINTER_WITHOUT_WHITESPACE.print(value));
        }
        catch(IOException e) {
            onError(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        closed = true;
        try {
            // As we lack supported trailers in standard HTTP, we'll have to make do with emitting an error to the
            // primary stream
            Optional<String> errorPayload = ErrorHandler.handleStreamingError(t);
            if(errorPayload.isPresent()) {
                write(errorPayload.get());
            }
            output.close();
        }
        catch(IOException e) {
            // Something really broke, try closing the connection.
            try {
                output.close();
            } catch (IOException e1) {
                // Ignored if we already have.
            }
        }
    }

    @Override
    public void onCompleted() {
        try {
            closed = true;
            output.close();
        }
        catch(IOException e) {
            onError(e);
        }
    }

    private void write(String value) throws IOException {
        if(value.isEmpty()) {
            return;
        }

        if(sse) {
            buffer.append("data: ");
        }

        buffer.append(value).append('\n');

        if(sse) {
            buffer.append('\n');
        }

        output.write(buffer.toString());

        // Reset buffer position to 0. At this point, the buffer will have a capacity of the max size(value) passed
        // through so far. In the majority of cases, other messages will be of similar (or larger) size,
        // so despite the fact that we might be holding onto a multi-mb buffer, it avoids the need continually
        // allocate new buffers per message
        buffer.setLength(0);
    }
}
