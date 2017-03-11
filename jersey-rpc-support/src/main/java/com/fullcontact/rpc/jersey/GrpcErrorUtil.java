package com.fullcontact.rpc.jersey;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.DebugInfo;
import com.google.rpc.RetryInfo;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import lombok.Value;

import javax.ws.rs.core.Response;

/**
 * Utilities for interfacing between HTTP/1.1 and GRPC
 *
 * Also includes error-handling logic
 *
 * @author Michael Rose (xorlev)
 */
public class GrpcErrorUtil {
    public static final Metadata.Key<DebugInfo> DEBUG_INFO_KEY = ProtoUtils.keyForProto(DebugInfo.getDefaultInstance());
    public static final Metadata.Key<RetryInfo> RETRY_INFO_KEY = ProtoUtils.keyForProto(RetryInfo.getDefaultInstance());

    public static int grpcToHttpStatus(io.grpc.Status status) {
        switch(status.getCode()) {
            case OK:
                return 200;
            case CANCELLED:
                return 503; // sort of
            case UNKNOWN:
                return 500;
            case INVALID_ARGUMENT:
                return 400;
            case DEADLINE_EXCEEDED:
                return 503; // eh?
            case NOT_FOUND:
                return 404;
            case ALREADY_EXISTS:
                return 422; // eh?
            case PERMISSION_DENIED:
                return 403;
            case RESOURCE_EXHAUSTED:
                return 503;
            case FAILED_PRECONDITION:
                return 412; // maybe
            case ABORTED:
                return 500;
            case OUT_OF_RANGE:
                return 416;
            case UNIMPLEMENTED:
                return 501;
            case INTERNAL:
                return 500;
            case UNAVAILABLE:
                return 503;
            case DATA_LOSS:
                return 500;
            case UNAUTHENTICATED:
                return 401;
        }

        return 0;
    }

    public static GrpcError throwableToStatus(Throwable t) {
        Status status = Status.fromThrowable(t);

        if(t instanceof InvalidProtocolBufferException) {
            status = Status.INVALID_ARGUMENT.withCause(t);
        }

        Metadata trailer = Status.trailersFromThrowable(t);

        int statusCode = grpcToHttpStatus(status);

        com.google.rpc.Status.Builder payload = com.google.rpc.Status.newBuilder();
        payload.setCode(status.getCode().value());

        StringBuilder errorMessage = new StringBuilder("HTTP " + statusCode + " (gRPC: "+status.getCode().name()+")");

        if(!Strings.isNullOrEmpty(status.getDescription())) {
            errorMessage.append(": ").append(Strings.nullToEmpty(status.getDescription()));
        }

        payload.setMessage(errorMessage.toString());

        if(trailer != null) {
            if(trailer.containsKey(RETRY_INFO_KEY)) {
                RetryInfo retryInfo = trailer.get(RETRY_INFO_KEY);
                payload.addDetails(Any.pack(retryInfo));
            }

            if(trailer.containsKey(DEBUG_INFO_KEY)) {
                DebugInfo debugInfo = trailer.get(DEBUG_INFO_KEY);
                payload.addDetails(Any.pack(debugInfo));
            }
        }

        return new GrpcError(
            status,
            payload.build()
        );
    }

    public static Response createJerseyResponse(Throwable t) {
        GrpcErrorUtil.GrpcError grpcError = GrpcErrorUtil.throwableToStatus(t);
        int httpStatusCode = GrpcErrorUtil.grpcToHttpStatus(grpcError.getStatus());

        Response.ResponseBuilder httpResponse = Response.status(httpStatusCode);

        try {
            for(Any extra : grpcError.getPayload().getDetailsList()) {
                if(extra.is(RetryInfo.class)) {
                    RetryInfo retryInfo = extra.unpack(RetryInfo.class);

                    if(retryInfo.hasRetryDelay()) {
                        httpResponse.header("Retry-After", Durations.toSeconds(retryInfo.getRetryDelay()));
                    }
                }
            }

            httpResponse.entity(JsonFormat.printer().print(grpcError.getPayload()));
        } catch(InvalidProtocolBufferException e) {
            // this should never happen
            throw new RuntimeException(e);
        }

        return httpResponse.build();
    }

    @Value
    static class GrpcError {
        Status status;
        com.google.rpc.Status payload;
    }

    private GrpcErrorUtil() {}
}
