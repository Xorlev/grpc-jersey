package com.fullcontact.rpc.jersey.util;

import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import java.util.Iterator;

/**
 * Utilities for dealing protobuf descriptors, specifically for generating names/java packages/classes just as protoc
 * does
 *
 * @author Michael Rose (xorlev)
 */
public class ProtobufDescriptorJavaUtil {
    private ProtobufDescriptorJavaUtil() {}

    public static String javaPackage(DescriptorProtos.FileDescriptorProto fd) {
        return fd.getOptions().getJavaPackage();
    }

    public static String grpcImplBaseClass(DescriptorProtos.FileDescriptorProto fd,
            DescriptorProtos.ServiceDescriptorProto sdp) {
        String serviceName = sdp.getName();

        return javaPackage(fd) + "." + serviceName + "Grpc." + serviceName + "ImplBase";
    }

    public static String grpcStubClass(DescriptorProtos.FileDescriptorProto fd,
            DescriptorProtos.ServiceDescriptorProto sdp) {
        String serviceName = sdp.getName();

        return javaPackage(fd) + "." + serviceName + "Grpc." + serviceName + "Stub";
    }

    public static String jerseyResourceClassName(DescriptorProtos.ServiceDescriptorProto sdp) {
        String serviceName = sdp.getName();

        return serviceName + "GrpcJerseyResource";
    }

    /**
     * Generate a class name from a descriptor. Handles options such as java_multiple_files, java_package, and
     * java_outer_classname
     */
    public static String genClassName(Descriptors.Descriptor descriptor) {
        String pkg = descriptor.getFile().getOptions().getJavaPackage();
        String outerClassName = descriptor.getFile().getOptions().getJavaOuterClassname();
        boolean multipleFiles = descriptor.getFile().getOptions().getJavaMultipleFiles();

        StringBuilder sb = new StringBuilder(pkg);
        sb.append(".");

        if (multipleFiles) {
            sb.append(descriptor.getName());
        } else {
            String baseClassName;
            if (!outerClassName.isEmpty()) {
                baseClassName = outerClassName;
            } else {
                String baseName = descriptor.getFile().getName();
                baseName = baseName.substring(baseName.lastIndexOf('/') + 1);
                baseName = baseName.replace(".proto", "").replace(".protodevel", "");
                baseClassName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, baseName);
            }

            sb.append(baseClassName)
                    .append(".")
                    .append(descriptor.getName());
        }

        return sb.toString();
    }

    public static ImmutableList<Descriptors.FieldDescriptor> fieldPath(Descriptors.Descriptor root, String path) {
        Iterable<String> pathSegments = Splitter.on('.').omitEmptyStrings().trimResults().split(path);

        Iterator<String> pathIterator = pathSegments.iterator();
        Descriptors.Descriptor descriptor = root;
        Descriptors.FieldDescriptor current;
        ImmutableList.Builder<Descriptors.FieldDescriptor> pathList = ImmutableList.builder();
        while (pathIterator.hasNext()) {
            String segment = pathIterator.next();
            current = descriptor.findFieldByName(segment);

            // not found, return empty
            if (current == null) {
                return ImmutableList.of();
            }

            if (current.getType() != Descriptors.FieldDescriptor.Type.MESSAGE && pathIterator.hasNext()) {
                throw new IllegalArgumentException("Found non-complex datatype at " + segment
                        + " in path: " + path + ": " + current);
            }

            if (current.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                descriptor = current.getMessageType();
            }

            pathList.add(current);
        }

        return pathList.build();
    }
}
