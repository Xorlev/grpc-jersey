package com.fullcontact.rpc.jersey.util;

import com.fullcontact.rpc.Test2;
import com.fullcontact.rpc.Test3Protos;
import com.fullcontact.rpc.TestRequest;

import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ProtobufDescriptorJavaUtil}
 *
 * @author Michael Rose (xorlev)
 */
public class ProtobufDescriptorJavaUtilTest {
    private final DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        TestRequest.getDescriptor().getFile().toProto();
    private final DescriptorProtos.ServiceDescriptorProto serviceDescriptorProto = DescriptorProtos.ServiceDescriptorProto
        .newBuilder()
        .setName("TestService")
        .build();

    @Test
    public void javaPackage() throws Exception {
        assertThat(ProtobufDescriptorJavaUtil.javaPackage(fileDescriptorProto))
            .isEqualTo("com.fullcontact.rpc");
    }

    @Test
    public void grpcImplBaseClass() throws Exception {
        String grpcImplBaseClass = ProtobufDescriptorJavaUtil.grpcImplBaseClass(fileDescriptorProto, serviceDescriptorProto);
        assertThat(grpcImplBaseClass)
                       .isEqualTo("com.fullcontact.rpc.TestServiceGrpc.TestServiceImplBase");

        // Ensure class exists
        Class.forName("com.fullcontact.rpc.TestServiceGrpc$TestServiceImplBase");
    }

    @Test
    public void grpcStubClass() throws Exception {
        assertThat(ProtobufDescriptorJavaUtil.grpcStubClass(fileDescriptorProto, serviceDescriptorProto))
            .isEqualTo("com.fullcontact.rpc.TestServiceGrpc.TestServiceStub");

        // Ensure class exists
        Class.forName("com.fullcontact.rpc.TestServiceGrpc$TestServiceStub");
    }

    @Test
    public void jerseyResourceClassName() throws Exception {
        assertThat(ProtobufDescriptorJavaUtil.jerseyResourceClassName(serviceDescriptorProto))
            .isEqualTo("TestServiceGrpcJerseyResource");
    }

    @Test
    public void genClassName() throws Exception {
        // multiple files, no custom class name
        assertThat(ProtobufDescriptorJavaUtil.genClassName(TestRequest.getDescriptor()))
            .isEqualTo("com.fullcontact.rpc.TestRequest");
        // single file, no custom class name
        assertThat(ProtobufDescriptorJavaUtil.genClassName(Test2.TestMessage2.getDescriptor()))
            .isEqualTo("com.fullcontact.rpc.Test2.TestMessage2");
        // single file, custom class name
        assertThat(ProtobufDescriptorJavaUtil.genClassName(Test3Protos.TestMessage3.getDescriptor()))
            .isEqualTo("com.fullcontact.rpc.Test3Protos.TestMessage3");
    }

    @Test
    public void fieldPath() throws Exception {
        assertThat(ProtobufDescriptorJavaUtil.fieldPath(TestRequest.getDescriptor(), "nt.nnt.f1"))
            .extracting(Descriptors.FieldDescriptor::getName)
            .isEqualTo(Lists.newArrayList("nt", "nnt", "f1"));
    }

    @Test
    public void fieldPath__badPath() throws Exception {
        assertThat(ProtobufDescriptorJavaUtil.fieldPath(TestRequest.getDescriptor(), "nt.nnt.f2"))
            .extracting(Descriptors.FieldDescriptor::getName)
            .isEmpty();
    }

}
