package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.jersey.util.ProtobufDescriptorJavaUtil;

import static com.fullcontact.rpc.jersey.util.ProtobufDescriptorJavaUtil.fieldPath;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.UnsafeByteOperations;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.Set;

/**
 * Utility used by resource template to parse parameters
 *
 * @author Michael Rose (xorlev)
 */
public class ParamParser {
    public static <V extends Message> void parseQueryParams(UriInfo uriInfo,
                                                            V.Builder builder,
                                                            DescriptorProtos.FieldDescriptorProto... pathParams) {
        Set<DescriptorProtos.FieldDescriptorProto> pathDescriptors = Sets.newHashSet(pathParams);

        for(Descriptors.FieldDescriptor fd : builder.getDescriptorForType().getFields()) {
            if(!pathDescriptors.contains(fd.toProto())) {
                String fieldName = fd.getName();
                String value = uriInfo.getQueryParameters().getFirst(fieldName);

                if(value == null || value.isEmpty())
                    continue;

                setFieldSafely(builder, fd, value);
            }
        }
    }

    public static  <T extends AbstractStub<T>> T parseHeaders(HttpHeaders headers, T stub){
        Metadata newHeaders = new Metadata();
        headers.getRequestHeaders().forEach((k, v) ->
            newHeaders.put(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v.get(0))
        );

        return MetadataUtils.attachHeaders(stub, newHeaders);
    }

    public static void setFieldSafely(Message.Builder builder, String path, String value) {
        Descriptors.Descriptor descriptor = builder.getDescriptorForType();

        ImmutableList<Descriptors.FieldDescriptor> fieldDescriptors =
            ProtobufDescriptorJavaUtil.fieldPath(descriptor, path);

        Message.Builder fieldBuilder = builder;
        for(Descriptors.FieldDescriptor fieldDescriptor : fieldDescriptors) {
            if(fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE)
                fieldBuilder = fieldBuilder.getFieldBuilder(fieldDescriptor);
        }

        setFieldSafely(fieldBuilder, fieldDescriptors.get(fieldDescriptors.size()-1), value);
    }

    public static void setFieldSafely(Message.Builder builder, Descriptors.FieldDescriptor fd, String value) {
        // TODO: strict validation
        switch(fd.getJavaType()) {
            case INT:
                builder.setField(fd, Integer.parseInt(value));
                break;
            case LONG:
                builder.setField(fd, Long.parseLong(value));
                break;
            case FLOAT:
                builder.setField(fd, Float.parseFloat(value));
                break;
            case DOUBLE:
                builder.setField(fd, Double.parseDouble(value));
                break;
            case BOOLEAN:
                builder.setField(fd, Boolean.parseBoolean(value));
                break;
            case STRING:
                builder.setField(fd, value);
                break;
            case BYTE_STRING:
                builder.setField(fd, UnsafeByteOperations.unsafeWrap(value.getBytes()));
                break;
            case ENUM:
                Descriptors.EnumValueDescriptor enumValueDescriptor =
                    fd.getEnumType().findValueByName(value.toUpperCase());
                builder.setField(fd, enumValueDescriptor); // TODO eh?
                break;
            case MESSAGE:
                // unsupported
                break;
        }
    }

    public static <V extends Message> void handleBody(
            String fieldPath,
            V.Builder builder,
            String body) throws InvalidProtocolBufferException {
        // * maps all body fields to the top-level proto
        // IDENT maps all body fields to nested proto
        // TODO: handle multiple levels of nesting
        Message.Builder toMerge = null;
        if("*".equals(fieldPath)) {
            toMerge = builder;
        } else {
            if(!fieldPath.contains(".")) {
                Descriptors.FieldDescriptor descriptor = builder.getDescriptorForType().findFieldByName(fieldPath);
                if(descriptor == null) {
                    // todo
                    return;
                }

                if(descriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                    toMerge = builder.getFieldBuilder(descriptor);
                }
            }
        }

        if(toMerge != null) {
            JsonFormat.parser().merge(body, toMerge);
        }
    }
}
