package com.fullcontact.rpc.jersey;

import java.io.IOException;
import java.util.Properties;

/**
 * Hold build metadata
 *
 * @author Michael Rose (xorlev)
 */
public class Build {
    private static final Properties properties;

    static {
        properties = new Properties();
        try {
            properties.load(Build.class.getResourceAsStream("/build.properties"));
        }
        catch(IOException e) {
            // ignore
        }
    }

    public static String version() {
        return properties.getProperty("version", "unknown");
    }

    private Build() {} // not instantiable
}
