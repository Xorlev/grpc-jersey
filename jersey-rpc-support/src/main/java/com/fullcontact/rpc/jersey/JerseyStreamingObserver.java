package com.fullcontact.rpc.jersey;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import org.glassfish.jersey.server.ChunkedOutput;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.io.IOException;
import java.util.List;

/**
 * gRPC StreamObserver which publishes to a Jersey ChunkedOutput
 *
 * @author Michael Rose (xorlev)
 */
public class JerseyStreamingObserver<V extends Message> implements StreamObserver<V> {
    public static final List<Variant> VARIANT_LIST = ImmutableList.of(
        new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, null),
        new Variant(new MediaType("text", "event-stream"), (String) null, null)
    );
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer()
                                                                .includingDefaultValueFields()
                                                                .omittingInsignificantWhitespace();

    private final ChunkedOutput<String> output;
    private final boolean sse;

    private volatile boolean closed = false;

    // Reusable buffer used in the context of a single streaming request
    private StringBuilder buffer = new StringBuilder();

    public JerseyStreamingObserver(ChunkedOutput<String> output, boolean sse) {
        this.output = output;
        this.sse = sse;
    }

    @Override
    public void onNext(V value) {
        if(closed)
            throw new IllegalStateException("StreamingObserver has already been closed");

        try {
            write(value);
        }
        catch(IOException e) {
            onError(e);
        }
    }

    private void write(MessageOrBuilder value) throws IOException {
        if(sse) {
            buffer.append("data: ");
        }

        buffer.append(PRINTER.print(value)).append('\n');

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

    @Override
    public void onError(Throwable t) {
        closed = true;
        try {
            // As we lack supported trailers in standard HTTP, we'll have to make do with emitting an error to the
            // primary stream
            write(GrpcErrorUtil.throwableToStatus(t).getPayload());
            output.close();
        }
        catch(IOException e) {
            // ignored
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
}
