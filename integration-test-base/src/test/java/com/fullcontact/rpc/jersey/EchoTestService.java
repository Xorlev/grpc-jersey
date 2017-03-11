package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.TestRequest;
import com.fullcontact.rpc.TestResponse;
import com.fullcontact.rpc.TestServiceGrpc;

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
        } else {
            responseObserver.onCompleted();
        }
    }
}
