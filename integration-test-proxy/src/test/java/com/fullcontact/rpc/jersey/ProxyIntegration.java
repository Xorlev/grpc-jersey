package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.TestServiceGrpc;
import com.fullcontact.rpc.TestServiceGrpcJerseyResource;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Proxy-based end-to-end Jersey tests
 *
 * @author Michael Rose (xorlev)
 */
@RunWith(JUnit4.class)
public class ProxyIntegration extends IntegrationBase {
    private static Server server = InProcessServerBuilder.forName("TestService")
            .addService(GrpcJerseyPlatformInterceptors.intercept(new EchoTestService()))
            .build();
    private static TestServiceGrpc.TestServiceStub stub =
            TestServiceGrpc.newStub(InProcessChannelBuilder
                    .forName("TestService")
                    .usePlaintext(true)
                    .directExecutor()
                    .build());
    @ClassRule
    public static final ResourceTestRule resources =
            ResourceTestRule.builder()
                    .addResource(new TestServiceGrpcJerseyResource(stub))
                    .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                    .build();

    static {
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ResourceTestRule resources() {
        return resources;
    }

    @Override
    public boolean supportsHttpHeaders() {
        return true;
    }
}
