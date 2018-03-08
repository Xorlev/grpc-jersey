package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.TestServiceGrpcJerseyResource;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Stub-based end-to-end Jersey tests
 *
 * @author Michael Rose (xorlev)
 */
@RunWith(JUnit4.class)
public class ServerStubIntegration extends IntegrationBase {
    @ClassRule
    public static final ResourceTestRule resources =
            ResourceTestRule.builder()
                    .addResource(new TestServiceGrpcJerseyResource(new EchoTestService()))
                    .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                    .build();

    @Override
    public ResourceTestRule resources() {
        return resources;
    }

    @Override
    public boolean supportsHttpHeaders() {
        return false;
    }
}
