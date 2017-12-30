package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.TestRequest;
import com.fullcontact.rpc.TestResponse;
import com.fullcontact.rpc.TestServiceGrpc;
import com.google.rpc.DebugInfo;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;

/**
 * gRPC service that echos the request into the response
 *
 * @author Michael Rose (xorlev)
 */
public class EchoTestService extends TestServiceGrpc.TestServiceImplBase {
    @Override
    public void testMethod(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        responseObserver.onNext(TestResponse.newBuilder().setRequest(request).build());
        responseObserver.onCompleted();
    }

    @Override
    public void testMethod2(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        responseObserver.onNext(TestResponse.newBuilder().setRequest(request).build());
        responseObserver.onCompleted();
    }

    @Override
    public void testMethod3(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        responseObserver.onNext(TestResponse.newBuilder().setRequest(request).build());
        responseObserver.onCompleted();
    }

    @Override
    public void testMethod4(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        responseObserver.onNext(TestResponse.newBuilder().setRequest(request).build());
        responseObserver.onCompleted();
    }

    @Override
    public void testMethod5(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        responseObserver.onNext(TestResponse.newBuilder().setRequest(request).build());
        responseObserver.onCompleted();
    }

    @Override
    public void testMethod6(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        responseObserver.onNext(TestResponse.newBuilder().setRequest(request).build());
        responseObserver.onCompleted();
    }

    @Override
    public void streamMethod1(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        for(int i = 0; i < request.getInt3(); i++) {
            responseObserver.onNext(TestResponse.newBuilder().setRequest(request).build());

            try {
                Thread.sleep(100);
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (request.getS().equals("explode")) {
            responseObserver.onError(new IllegalStateException("Explode called."));
        } else if (request.getS().equals("grpc_data_loss")) {
            Metadata metadata = new Metadata();
            metadata.put(GrpcErrorUtil.DEBUG_INFO_KEY,
                    DebugInfo.newBuilder().setDetail("test2").build());

            io.grpc.Status status = io.grpc.Status.DATA_LOSS
                    .withCause(new IllegalStateException("Grue detected."))
                    .withDescription("Fail-fast: Grue found in write-path.")
                    .augmentDescription("test");

            responseObserver.onError(status.asRuntimeException(metadata));
        } else {
            responseObserver.onCompleted();
        }
    }
}
