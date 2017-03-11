package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.TestServiceGrpc;
import com.fullcontact.rpc.TestServiceGrpcJerseyResource;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/**
 * Dropwizard App for local testing
 *
 * @author Michael Rose (xorlev)
 */
public class ProxyIntegrationApp extends Application<Configuration> {
    public static void main(String[] args) throws Exception {
        new ProxyIntegrationApp().run(args);
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
        Server server = InProcessServerBuilder.forName("TestService")
                                              .addService(new EchoTestService())
                                              .build();
        server.start();

        TestServiceGrpc.TestServiceStub stub =
            TestServiceGrpc.newStub(InProcessChannelBuilder
                                        .forName("TestService")
                                        .usePlaintext(true)
                                        .directExecutor()
                                        .build());

        environment.jersey().register(new TestServiceGrpcJerseyResource(stub));
    }
}
