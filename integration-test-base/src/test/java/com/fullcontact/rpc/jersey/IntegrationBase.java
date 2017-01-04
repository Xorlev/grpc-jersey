package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.NestedType;
import com.fullcontact.rpc.TestEnum;
import com.fullcontact.rpc.TestRequest;
import com.fullcontact.rpc.TestResponse;

import com.google.protobuf.util.JsonFormat;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.ws.rs.client.Entity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base end-to-end Jersey tests
 *
 * @author Michael Rose (xorlev)
 */
@RunWith(JUnit4.class)
public abstract class IntegrationBase {
    public abstract ResourceTestRule resources();

    @Test
    public void testBasicGet() throws Exception {
        // /users/{s}/{uint3}/{nt.f1}
        String responseJson = resources().getJerseyTest()
                                                 .target("/users/string1/1234/abcd")
                                                 .request()
                                                 .buildGet()
                                                 .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getS()).isEqualTo("string1");
        assertThat(response.getRequest().getUint3()).isEqualTo(1234);
        assertThat(response.getRequest().getNt().getF1()).isEqualTo("abcd");
    }

    @Test
    public void testBasicPost() throws Exception {
        TestRequest request = TestRequest.newBuilder()
            .setBoolean(true)
            .setS("Hello")
            .setNt(NestedType.newBuilder().setF1("World"))
            .build();
        String responseJson = resources().getJerseyTest()
                 .target("/users/")
                 .request()
                 .buildPost(Entity.entity(JsonFormat.printer().print(request), "application/json; charset=utf-8"))
                 .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest()).isEqualTo(request);
    }

    @Test
    public void testPost__nestedBinding() throws Exception {
        NestedType request = NestedType.newBuilder().setF1("World").build();
        String responseJson = resources().getJerseyTest()
                 .target("/users_nested/")
                 .request()
                 .buildPost(Entity.entity(JsonFormat.printer().print(request),
                                          "application/json; charset=utf-8"))
                 .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getNt()).isEqualTo(request);
    }

    @Test
    public void testAdvancedGet() throws Exception {
        // /users/{s=hello/**}/x/{uint3}/{nt.f1}/*/**/test
        String responseJson = resources().getJerseyTest()
                                       .target("/users/hello/string1/test/x/1234/abcd/foo/bar/baz/test")
                                       .queryParam("d", 1234.5678)
                                       .queryParam("enu", "SECOND")
                                       .queryParam("uint3", "5678") // ensure path param has precedence
                                       .queryParam("x", "y")
                                       .request()
                                       .buildGet()
                                       .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getS()).isEqualTo("hello/string1/test");
        assertThat(response.getRequest().getUint3()).isEqualTo(1234);
        assertThat(response.getRequest().getD()).isEqualTo(1234.5678);
        assertThat(response.getRequest().getEnu()).isEqualTo(TestEnum.SECOND);
        assertThat(response.getRequest().getNt().getF1()).isEqualTo("abcd");
    }

    @Test
    public void testAdvancedGetFromYaml() throws Exception {
        // /yaml_users/{s=hello/**}/x/{uint3}/{nt.f1}/*/**/test
        String responseJson = resources().getJerseyTest()
            .target("/yaml_users/hello/string1/test/x/1234/abcd/foo/bar/baz/test")
            .queryParam("d", 1234.5678)
            .queryParam("enu", "SECOND")
            .queryParam("uint3", "5678") // ensure path param has precedence
            .queryParam("x", "y")
            .request()
            .buildGet()
            .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getS()).isEqualTo("hello/string1/test");
        assertThat(response.getRequest().getUint3()).isEqualTo(1234);
        assertThat(response.getRequest().getD()).isEqualTo(1234.5678);
        assertThat(response.getRequest().getEnu()).isEqualTo(TestEnum.SECOND);
        assertThat(response.getRequest().getNt().getF1()).isEqualTo("abcd");
    }

    @Test
    public void testBasicGetFromYaml() throws Exception {
        // /yaml_users/{s}/{uint3}/{nt.f1}
        String responseJson = resources().getJerseyTest()
            .target("/yaml_users/string1/1234/abcd")
            .request()
            .buildGet()
            .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getS()).isEqualTo("string1");
        assertThat(response.getRequest().getUint3()).isEqualTo(1234);
        assertThat(response.getRequest().getNt().getF1()).isEqualTo("abcd");
        assertThat(false);
    }

    @Test
    public void testBasicPostYaml() throws Exception {
        TestRequest request = TestRequest.newBuilder()
            .setBoolean(true)
            .setS("Hello")
            .setNt(NestedType.newBuilder().setF1("World"))
            .build();
        String responseJson = resources().getJerseyTest()
            .target("/yaml_users/")
            .request()
            .buildPost(Entity.entity(JsonFormat.printer().print(request), "application/json; charset=utf-8"))
            .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest()).isEqualTo(request);
    }

    @Test
    public void testPost__nestedBindingYaml() throws Exception {
        NestedType request = NestedType.newBuilder().setF1("World").build();
        String responseJson = resources().getJerseyTest()
            .target("/yaml_users_nested/")
            .request()
            .buildPost(Entity.entity(JsonFormat.printer().print(request),
                "application/json; charset=utf-8"))
            .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getNt()).isEqualTo(request);
    }


}
