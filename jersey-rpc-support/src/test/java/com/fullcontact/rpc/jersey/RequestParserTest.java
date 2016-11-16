package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.NestedNestedType;
import com.fullcontact.rpc.NestedType;
import com.fullcontact.rpc.TestEnum;
import com.fullcontact.rpc.TestRequest;
import com.fullcontact.rpc.jersey.util.ProtobufDescriptorJavaUtil;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.util.JsonFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link RequestParser}
 *
 * @author Michael Rose (xorlev)
 */
@RunWith(JUnit4.class)
public class RequestParserTest {
    @Test
    public void parseQueryParams() throws Exception {
        TestRequest.Builder request = TestRequest.newBuilder();

        UriInfoMap uriInfoMap = new UriInfoMap()
            .put("s", "string")
            .put("bytearray", "string")
            .put("boolean", "true")
//            .put("nt.f1", "2") TODO(xorlev): support nested queryparam expansion
            .put("uint3", "3000000000")
            .put("int3", "2000000000")
            .put("uint6", "10000000000000000000")
            .put("int6", "9000000000000000000")
            .put("f", "123.456")
            .put("d", "123.456")
            .put("enu", "SECOND");

        RequestParser.parseQueryParams(uriInfoMap, request);

        TestRequest r = request.build();

        assertThat(r.getS()).isEqualTo("string");
        assertThat(r.getBytearray().toStringUtf8()).isEqualTo("string");
        assertThat(r.getBoolean()).isTrue();
//        assertThat(r.getNt().getF1()).isEqualTo("2"); TODO(xorlev): support nested queryparam expansion
        assertThat(r.getUint3()).isEqualTo(-1294967296); // uint{32,64} are stored "signed" in Java
        assertThat(r.getInt3()).isEqualTo(2000000000);
        assertThat(r.getUint6()).isEqualTo(-8446744073709551616L);
        assertThat(r.getInt6()).isEqualTo(9000000000000000000L);
        assertThat(r.getF()).isEqualTo(123.456f);
        assertThat(r.getD()).isEqualTo(123.456d);
        assertThat(r.getEnu()).isEqualTo(TestEnum.SECOND);
    }

    @Test
    public void parseHeaders() throws Exception {

    }

    @Test
    public void setFieldSafely() throws Exception {
        TestRequest.Builder request = TestRequest.newBuilder();
        RequestParser.setFieldSafely(request, "nt.f1", "abc");

        assertThat(request.build().getNt().getF1()).isEqualTo("abc");
    }

    @Test
    public void handleBody() throws Exception {
        TestRequest.Builder request = TestRequest.newBuilder();
        request.setS("string");
        request.setBytearray(ByteString.copyFromUtf8("string"));
        request.setUint3(123);
        request.setUint6(123);
        request.setInt3(123);
        request.setInt6(123);
        request.setF(123.456f);
        request.setD(123.456d);
        request.setEnu(TestEnum.SECOND);
        request.setBoolean(true);
        request.setNt(NestedType.newBuilder().setF1("abc").build());

        String json = JsonFormat.printer().print(request);

        TestRequest.Builder deserialized = TestRequest.newBuilder();
        RequestParser.handleBody("*", deserialized, json);
        assertThat(deserialized.build()).isEqualTo(request.build());
    }

    @Test
    public void handleBody__nested() throws Exception {
        NestedType nestedType = NestedType.newBuilder().setF1("abc").build();
        TestRequest expected = TestRequest.newBuilder().setNt(nestedType).build();

        // Output nested json only
        String json = JsonFormat.printer().print(nestedType);

        TestRequest.Builder deserialized = TestRequest.newBuilder();
        RequestParser.handleBody("nt", deserialized, json);
        assertThat(deserialized.build()).isEqualTo(expected);
    }

    @Test
    public void handleBody__nested2() throws Exception {
        NestedNestedType nestedType = NestedNestedType.newBuilder().addF1("abc").build();
        TestRequest expected = TestRequest.newBuilder()
                                          .setNt(NestedType.newBuilder().setNnt(nestedType).build())
                                          .build();

        // Output nested json only
        String json = JsonFormat.printer().print(nestedType);

        TestRequest.Builder deserialized = TestRequest.newBuilder();
        RequestParser.handleBody("nt.nnt", deserialized, json);
        assertThat(deserialized.build()).isEqualTo(expected);
    }

}
