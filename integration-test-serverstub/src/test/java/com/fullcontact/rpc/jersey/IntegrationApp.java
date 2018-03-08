package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.TestServiceGrpcJerseyResource;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

/**
 * Dropwizard App for local testing
 *
 * @author Michael Rose (xorlev)
 */
public class IntegrationApp extends Application<Configuration> {
    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
        environment.jersey().register(new TestServiceGrpcJerseyResource(new EchoTestService()));
    }

    public static void main(String[] args) throws Exception {
        new IntegrationApp().run(args);
    }
}
