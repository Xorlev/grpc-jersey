package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.NestedType;
import com.fullcontact.rpc.TestEnum;
import com.fullcontact.rpc.TestRequest;
import com.fullcontact.rpc.TestResponse;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.Status;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.assertj.core.util.Strings;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.message.internal.EntityInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.ws.rs.client.Entity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

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
    public void testBasicGetWith1RepeatedIntParam() throws Exception {
        // /users/{s}/{uint3}/{nt.f1}?rep=1&rep=2&rep=3
        String responseJson = resources().getJerseyTest()
                                                 .target("/users/string1/1234/abcd")
                                                 .queryParam("rep", 1)
                                                 .request()
                                                 .buildGet()
                                                 .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getS()).isEqualTo("string1");
        assertThat(response.getRequest().getUint3()).isEqualTo(1234);
        assertThat(response.getRequest().getNt().getF1()).isEqualTo("abcd");
        assertThat(response.getRequest().getRepList()).isEqualTo(ImmutableList.of(1));
    }

    @Test
    public void testBasicGetWithRepeatedIntParam() throws Exception {
        // /users/{s}/{uint3}/{nt.f1}?rep=1&rep=2&rep=3
        String responseJson = resources().getJerseyTest()
                                                 .target("/users/string1/1234/abcd")
                                                 .queryParam("rep", 1, 2, 3)
                                                 .request()
                                                 .buildGet()
                                                 .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getS()).isEqualTo("string1");
        assertThat(response.getRequest().getUint3()).isEqualTo(1234);
        assertThat(response.getRequest().getNt().getF1()).isEqualTo("abcd");
        assertThat(response.getRequest().getRepList()).isEqualTo(ImmutableList.of(1, 2, 3));
    }

    @Test
    public void testBasicGetWithRepeatedStrParam() throws Exception {
        // /users/{s}/{uint3}/{nt.f1}?repStr=a&repStr=b&repStr=c
        String responseJson = resources().getJerseyTest()
                                                 .target("/users/string1/1234/abcd")
                                                 .queryParam("rep_str", "a", "b", "c")
                                                 .request()
                                                 .buildGet()
                                                 .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getS()).isEqualTo("string1");
        assertThat(response.getRequest().getUint3()).isEqualTo(1234);
        assertThat(response.getRequest().getNt().getF1()).isEqualTo("abcd");
        assertThat(response.getRequest().getRepStrList()).isEqualTo(ImmutableList.of("a", "b", "c"));
    }

    @Test
    public void testBasicGetWithRepeatedEmptyStrParam() throws Exception {
        // /users/{s}/{uint3}/{nt.f1}?repStr=a&repStr=&repStr=b&repStr=
        String responseJson = resources().getJerseyTest()
                                                 .target("/users/string1/1234/abcd")
                                                 .queryParam("rep_str", "a", "", "b", "")
                                                 .request()
                                                 .buildGet()
                                                 .invoke(String.class);

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();

        assertThat(response.getRequest().getS()).isEqualTo("string1");
        assertThat(response.getRequest().getUint3()).isEqualTo(1234);
        assertThat(response.getRequest().getNt().getF1()).isEqualTo("abcd");
        assertThat(response.getRequest().getRepStrList()).isEqualTo(ImmutableList.of("a", "", "b", ""));
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
    public void testAdvancedGet__defaultEnumInResponse() throws Exception {
        // /users/{s=hello/**}/x/{uint3}/{nt.f1}/*/**/test
        String responseJson = resources().getJerseyTest()
                                       .target("/users/hello/string1/test/x/1234/abcd/foo/bar/baz/test")
                                       .queryParam("d", 1234.5678)
                                       .queryParam("enu", "FIRST")
                                       .queryParam("uint3", "5678") // ensure path param has precedence
                                       .queryParam("x", "y")
                                       .request()
                                       .buildGet()
                                       .invoke(String.class);

        // We want to ensure this is always set despite the fact that FIRST=0 which is not normally serialized.
        // Since this is intended to work with other systems (such as frontends or non-Java systems without compiled
        // protos) we want to ensure the structure remains relatively the same.
        assertThat(responseJson).contains("\"enu\": \"FIRST\"");

        TestResponse.Builder responseFromJson = TestResponse.newBuilder();
        JsonFormat.parser().merge(responseJson, responseFromJson);
        TestResponse response = responseFromJson.build();
        assertThat(response.getRequest().getEnu()).isEqualTo(TestEnum.FIRST);
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

    public void testStreamGet() throws Exception {
        InputStream response = resources().getJerseyTest()
                                       .target("/stream/hello")
                                       .queryParam("d", 1234.5678)
                                       .queryParam("enu", "SECOND")
                                       .queryParam("int3", "10")
                                       .queryParam("x", "y")
                                       .queryParam("nt.f1", "abcd")
                                       .request()
                                       .buildGet()
                                       .invoke(InputStream.class);

        BufferedReader reader = new BufferedReader(new InputStreamReader(response));

        int count = 0;
        long now = System.currentTimeMillis();
        while(true) {
            String json = reader.readLine();

            if(Strings.isNullOrEmpty(json))
                break;

            TestResponse.Builder responseFromJson = TestResponse.newBuilder();
            JsonFormat.parser().merge(json, responseFromJson);
            TestResponse r = responseFromJson.build();

            assertThat(r.getRequest().getS()).isEqualTo("hello");
            assertThat(r.getRequest().getInt3()).isEqualTo(10);
            assertThat(r.getRequest().getD()).isEqualTo(1234.5678);
            assertThat(r.getRequest().getEnu()).isEqualTo(TestEnum.SECOND);
            assertThat(r.getRequest().getNt().getF1()).isEqualTo("abcd");

            count++;

            long after = System.currentTimeMillis();
            long duration = after - now;

            // This might be flaky, but we want to ensure that we're actually streaming
            assertThat(duration).isLessThan(1000/2);
            now = after;
        }

        assertThat(count).isEqualTo(10);
    }

    @Test
    public void testStreamGetError() throws Exception {
        InputStream response = resources().getJerseyTest()
                                       .target("/stream/explode")
                                       .queryParam("d", 1234.5678)
                                       .queryParam("enu", "SECOND")
                                       .queryParam("int3", "10")
                                       .queryParam("x", "y")
                                       .queryParam("nt.f1", "abcd")
                                       .request()
                                       .buildGet()
                                       .invoke(InputStream.class);

        BufferedReader reader = new BufferedReader(new InputStreamReader(response));

        // int3 controls "successful" messages. Next request will throw.
        for (int i = 0; i < 10; i++) {
            String json = reader.readLine();

            if(Strings.isNullOrEmpty(json))
                break;

            TestResponse.Builder responseFromJson = TestResponse.newBuilder();
            JsonFormat.parser().merge(json, responseFromJson);
            TestResponse r = responseFromJson.build();

            assertThat(r.getRequest().getS()).isEqualTo("explode");
        }

        String json = reader.readLine();
        Status.Builder statusBuilder = Status.newBuilder();
        JsonFormat.parser().merge(json, statusBuilder);

        Status expected = Status
                .newBuilder()
                .setCode(2)
                .setMessage("HTTP 500 (gRPC: UNKNOWN)")
                .build();

        assertThat(statusBuilder.build()).isEqualTo(expected);
    }
}
