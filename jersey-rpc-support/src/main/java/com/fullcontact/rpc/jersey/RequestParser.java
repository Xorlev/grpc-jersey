package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.jersey.util.ProtobufDescriptorJavaUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.UnsafeByteOperations;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility used by resource template to parse parameters
 *
 * @author Michael Rose (xorlev)
 */
public class RequestParser {
    public static <V extends Message> void parseQueryParams(UriInfo uriInfo,
                                                            V.Builder builder,
                                                            DescriptorProtos.FieldDescriptorProto... pathParams)
            throws InvalidProtocolBufferException {
        parseQueryParams(uriInfo, builder, ImmutableList.copyOf(pathParams));
    }
    public static <V extends Message> void parseQueryParams(UriInfo uriInfo,
                                                            V.Builder builder,
                                                            List<DescriptorProtos.FieldDescriptorProto> pathParams)
            throws InvalidProtocolBufferException {
        Set<DescriptorProtos.FieldDescriptorProto> pathDescriptors = Sets.newHashSet(pathParams);

        for(String queryParam : uriInfo.getQueryParameters().keySet()) {
            ImmutableList<Descriptors.FieldDescriptor> descriptors =
                ProtobufDescriptorJavaUtil.fieldPath(builder.getDescriptorForType(), queryParam);
            if(!descriptors.isEmpty()) {
                Descriptors.FieldDescriptor field = Iterables.getLast(descriptors);

                if(!pathDescriptors.contains(field.toProto())) {
                    setFieldSafely(builder, queryParam, uriInfo.getQueryParameters().get(queryParam));
                }
            }
        }
    }

    public static  <T extends AbstractStub<T>> T parseHeaders(HttpHeaders headers, T stub){
        return MetadataUtils.attachHeaders(stub, parseHeaders(headers));
    }

    public static Metadata parseHeaders(HttpHeaders headers){
        Metadata newHeaders = new Metadata();
        headers.getRequestHeaders().forEach((k, v) ->
            newHeaders.put(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v.get(0))
        );

        return newHeaders;
    }

    public static void setFieldSafely(Message.Builder builder, String path, String value)
            throws InvalidProtocolBufferException {
        setFieldSafely(builder, path, ImmutableList.of(value));
    }
    
    public static void setFieldSafely(Message.Builder builder, String path, List<String> value)
            throws InvalidProtocolBufferException {
        Descriptors.Descriptor descriptor = builder.getDescriptorForType();

        ImmutableList<Descriptors.FieldDescriptor> fieldDescriptors =
            ProtobufDescriptorJavaUtil.fieldPath(descriptor, path);

        Message.Builder fieldBuilder = builder;
        for(Descriptors.FieldDescriptor fieldDescriptor : fieldDescriptors) {
            if(fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE)
                fieldBuilder = fieldBuilder.getFieldBuilder(fieldDescriptor);
        }

        if(fieldDescriptors.isEmpty()) {
            throw new IllegalArgumentException("Path " + path + " doesn't exist from root: "
                                               + builder.getDescriptorForType().getName());
        }

        setFieldSafely(fieldBuilder, fieldDescriptors.get(fieldDescriptors.size()-1), value);
    }

    public static void setFieldSafely(Message.Builder builder, Descriptors.FieldDescriptor fd, List<String> value)
    throws InvalidProtocolBufferException {
        Object valueToSet = getValueFor(fd, value);
        builder.setField(fd, valueToSet);
    }

    private static Object getValueFor(FieldDescriptor fd, List<String> value) throws InvalidProtocolBufferException {
        Object result;
        if (!fd.isRepeated()) {
            if (value.size() != 1) {
                throw new InvalidProtocolBufferException("Unable to map " + fd + " to value: " + value);
            }
            result = getUnaryValueFor(fd, value.get(0));
        } else {
            List<Object> listResult = new ArrayList<>(value.size());
            for (String valueStr : value) {
                listResult.add(getUnaryValueFor(fd, valueStr));
            }
            result = listResult;
        }
        return result;
    }
    
    private static Object getUnaryValueFor(Descriptors.FieldDescriptor fd, String value) throws InvalidProtocolBufferException {
        try {
            switch(fd.getType()) {
                case DOUBLE:
                    return Double.parseDouble(value);
                case FLOAT:
                    return Float.parseFloat(value);
                case BOOL:
                    return Boolean.parseBoolean(value);
                case STRING:
                    return value;
                case BYTES:
                    return UnsafeByteOperations.unsafeWrap(value.getBytes());
                case ENUM:
                    Descriptors.EnumValueDescriptor enumValueDescriptor =
                        fd.getEnumType().findValueByName(value.toUpperCase());
                    return enumValueDescriptor; // TODO eh?
                case INT32:
                    return Integer.parseInt(value);
                case UINT32:
                case FIXED32:
                case SFIXED32:
                case SINT32:
                    return Integer.parseUnsignedInt(value);
                case INT64:
                    return Long.parseLong(value);
                case UINT64:
                case FIXED64:
                case SFIXED64:
                case SINT64:
                    return Long.parseUnsignedLong(value);
                    // all are unsigned 64-bit ints
                case GROUP:
                    // unsupported
                case MESSAGE:
                    // unsupported
                default:
                    throw new InvalidProtocolBufferException("Unable to map " + fd + " to value: " + value);
            }
        } catch(NumberFormatException e) {
            throw new InvalidProtocolBufferException("Unable to map " + fd + " to value: " + value);
        }
    }
    
    public static <V extends Message> void handleBody(
            String fieldPath,
            V.Builder builder,
            String body) throws InvalidProtocolBufferException {
        // * maps all body fields to the top-level proto
        // IDENT maps all body fields to nested proto
        // TODO: handle multiple levels of nesting
        Message.Builder toMerge;
        if("*".equals(fieldPath)) {
            toMerge = builder;
        } else {
            ImmutableList<Descriptors.FieldDescriptor> fieldDescriptors =
                ProtobufDescriptorJavaUtil.fieldPath(builder.getDescriptorForType(), fieldPath);

            if(fieldDescriptors.isEmpty()) {
                // todo bad request
                return;
            }

            toMerge = builder;
            for(Descriptors.FieldDescriptor fd : fieldDescriptors) {
                if(fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                    toMerge = toMerge.getFieldBuilder(fd);
                }
            }
        }

        if(toMerge != null) {
            JsonFormat.parser().merge(body, toMerge);
        }
    }
}
