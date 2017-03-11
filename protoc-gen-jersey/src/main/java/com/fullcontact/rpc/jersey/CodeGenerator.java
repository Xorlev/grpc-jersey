package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.jersey.util.ProtobufDescriptorJavaUtil;

import com.fullcontact.rpc.jersey.yaml.YamlHttpConfig;
import com.fullcontact.rpc.jersey.yaml.YamlHttpRule;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.*;
import com.google.protobuf.compiler.PluginProtos;
import lombok.Builder;
import lombok.Value;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Jersey JSON/proto REST gRPC gateway compiler
 *
 * For every gRPC method annotated with google.api.http options, this compiler emits a Jersey resource method.
 *
 * @author Michael Rose (xorlev)
 */
public class CodeGenerator {

    public PluginProtos.CodeGeneratorResponse generate(PluginProtos.CodeGeneratorRequest request)
            throws Descriptors.DescriptorValidationException {
        Set<String> options = Sets.newHashSet(Splitter.on(',').split(request.getParameter()));

        boolean isProxy = options.contains("proxy");

        Map<String, Descriptors.Descriptor> lookup = new HashMap<>();
        PluginProtos.CodeGeneratorResponse.Builder response = PluginProtos.CodeGeneratorResponse.newBuilder();

        List<Descriptors.FileDescriptor> fileDescriptors = Lists.newArrayList(
            DescriptorProtos.MethodOptions.getDescriptor().getFile(),
            AnnotationsProto.getDescriptor(),
            HttpRule.getDescriptor().getFile()
        );

        Optional<YamlHttpConfig> yamlConfig = YamlHttpConfig.getFromOptions(options);

        for(DescriptorProtos.FileDescriptorProto fdProto : request.getProtoFileList()) {
            // Descriptors are provided in dependency-topological order
            // each time we collect a new FileDescriptor, we add it to a
            // mutable list of descriptors and append the entire dependency
            // chain to each new FileDescriptor to allow crossLink() to function.
            // TODO(xorlev): might have to be more selective about deps in future
            Descriptors.FileDescriptor fd = Descriptors.FileDescriptor.buildFrom(
                fdProto, fileDescriptors.toArray(new Descriptors.FileDescriptor[] {})
            );
            fileDescriptors.add(fd);

            // if type starts with a ".", it's in this package
            // otherwise it's fully qualified
            String protoPackage = fdProto.getPackage();
            for(DescriptorProtos.DescriptorProto d : fdProto.getMessageTypeList()) {
                String prefix = ".";

                if(!Strings.isNullOrEmpty(protoPackage)) {
                    prefix += protoPackage + ".";
                }

                lookup.put(prefix + d.getName(), fd.findMessageTypeByName(d.getName()));
            }

            // Find RPC methods with HTTP extensions
            List<ServiceAndMethod> methodsToGenerate = new ArrayList<>();
            for(Descriptors.ServiceDescriptor serviceDescriptor : fd.getServices()) {
                DescriptorProtos.ServiceDescriptorProto serviceDescriptorProto = serviceDescriptor.toProto();
                for(DescriptorProtos.MethodDescriptorProto methodProto : serviceDescriptorProto.getMethodList()) {
                    String fullMethodName = serviceDescriptor.getFullName() +"." + methodProto.getName();
                    if(yamlConfig.isPresent()) {   //Check to see if the rules are defined in the YAML
                        for(YamlHttpRule rule : yamlConfig.get().getRules()) {
                            if(rule.getSelector().equals(fullMethodName) || rule.getSelector().equals("*")) { //TODO:  com.foo.*
                                DescriptorProtos.MethodOptions yamlOptions = DescriptorProtos.MethodOptions.newBuilder()
                                    .setExtension(AnnotationsProto.http, rule.buildHttpRule())
                                    .build();
                                methodProto = DescriptorProtos.MethodDescriptorProto.newBuilder()
                                    .mergeFrom(methodProto)
                                    .setOptions(yamlOptions)
                                    .build();
                            }
                        }
                    }
                    if(methodProto.getOptions().hasExtension(AnnotationsProto.http)) {
                        // TODO(xorlev): support server streaming
                        if(methodProto.getServerStreaming() || methodProto.getClientStreaming())
                            throw new IllegalArgumentException("http annotations cannot be used with streaming methods");

                        methodsToGenerate.add(new ServiceAndMethod(serviceDescriptor, methodProto));
                    }
                }
            }
            if(!methodsToGenerate.isEmpty())
                generateResource(response, lookup, fdProto, methodsToGenerate, isProxy);
        }

        return response.build();
    }

    private void generateResource(
            PluginProtos.CodeGeneratorResponse.Builder response,
            Map<String, Descriptors.Descriptor> descriptorTable,
            DescriptorProtos.FileDescriptorProto fileDescriptorProto,
            List<ServiceAndMethod> generate,
            boolean isProxy) {
        ResourceToGenerate r = buildResourceSpec(descriptorTable, fileDescriptorProto, generate, isProxy);

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("resource.tmpl.java");
        StringWriter writer = new StringWriter();
        mustache.execute(writer, r);

        response.addFile(PluginProtos.CodeGeneratorResponse.File.newBuilder()
                         .setContent(writer.toString())
                         .setName(r.getFileName())
                         .build());

        System.err.println(writer.toString());
    }

    /**
     * Creates a generator spec for a given service resource and all of the methods.
     *
     * @param descriptorTable mapping of proto.path.MessageType to the descriptor instance
     * @param fileDescriptorProto file descriptor of the origin service
     * @param methodSpecs list of methods in the given service
     * @param isProxy should this resource use client stubs or implbase?
     * @return
     */
    @VisibleForTesting
    ResourceToGenerate buildResourceSpec(
        Map<String, Descriptors.Descriptor> descriptorTable,
        DescriptorProtos.FileDescriptorProto fileDescriptorProto,
        List<ServiceAndMethod> methodSpecs,
        boolean isProxy) {
        Descriptors.ServiceDescriptor serviceDescriptor = methodSpecs.get(0).getServiceDescriptor();
        DescriptorProtos.ServiceDescriptorProto sdp = methodSpecs.get(0).getServiceDescriptor().toProto();
        String packageName = ProtobufDescriptorJavaUtil.javaPackage(fileDescriptorProto);
        String className = ProtobufDescriptorJavaUtil.jerseyResourceClassName(sdp);
        String grpcImplClass = (isProxy)?
                               ProtobufDescriptorJavaUtil.grpcStubClass(fileDescriptorProto, sdp):
                               ProtobufDescriptorJavaUtil.grpcImplBaseClass(fileDescriptorProto, sdp);
        String fileName = packageName.replace('.', '/') + "/" + className + ".java";

        ImmutableList.Builder<ResourceMethodToGenerate> methods = ImmutableList.builder();
        for(ServiceAndMethod sam : methodSpecs) {
            Descriptors.Descriptor inputDescriptor = descriptorTable.get(sam.getMethodDescriptor().getInputType());
            Descriptors.Descriptor outputDescriptor = descriptorTable.get(sam.getMethodDescriptor().getOutputType());
            List<ResourceMethodToGenerate> methodToGenerate = parseRule(sam, inputDescriptor, outputDescriptor);
            methods.addAll(methodToGenerate);
        }

        return ResourceToGenerate
            .builder()
            .serviceDescriptor(serviceDescriptor)
            .javaPackage(ProtobufDescriptorJavaUtil.javaPackage(fileDescriptorProto))
            .className(className)
            .grpcStub(grpcImplClass)
            .methods(methods.build())
            .parseHeaders(isProxy)
            .fileName(fileName)
            .build();
    }

    /**
     * Generates a {@link ResourceMethodToGenerate} spec from the {@link ServiceAndMethod} plus the input/output RPC
     * message descriptors
     *
     * @param sam named tuple of (service descriptor proto, method descriptor proto)
     * @param inputDescriptor RPC input descriptor
     * @param outputDescriptor RPC output descriptor
     * @return list of handlers to handle the given RPC method. Usually a single result, but can be multiple if
     * additional_bindings is defined.
     */
    @VisibleForTesting
    ImmutableList<ResourceMethodToGenerate> parseRule(ServiceAndMethod sam,
                                                      Descriptors.Descriptor inputDescriptor,
                                                      Descriptors.Descriptor outputDescriptor) {
        HttpRule baseRule = sam.getMethodDescriptor().getOptions().getExtension(AnnotationsProto.http);

        ImmutableList<HttpRule> rules = ImmutableList.<HttpRule>builder()
            .add(baseRule)
            .addAll(baseRule.getAdditionalBindingsList())
            .build();

        ImmutableList.Builder<ResourceMethodToGenerate> methodsToGenerate = ImmutableList.builder();
        int methodIndex = 0;
        for(HttpRule rule : rules) {
            String method = rule.getPatternCase().toString();
            String path = "";
            switch(rule.getPatternCase()) {
                case GET:
                    path = rule.getGet();
                    break;
                case PUT:
                    path = rule.getPut();
                    break;
                case POST:
                    path = rule.getPost();
                    break;
                case DELETE:
                    path = rule.getDelete();
                    break;
                case PATCH:
                    path = rule.getPatch();
                    break;
                case CUSTOM:
                    throw new IllegalArgumentException("Jersey compiler does not support custom HTTP verbs.");
                case PATTERN_NOT_SET:
                    throw new IllegalArgumentException("Pattern (GET,PUT,POST,DELETE,PATCH) must be set.");
            }

            if(path.trim().isEmpty())
                throw new IllegalArgumentException("rule path must be set");

            // TODO(xorlev): check for URL overlap
            PathParser.ParsedPath parsedPath = PathParser.parse(path);
            ImmutableList<PathParam> pathParams = parsePathParams(inputDescriptor, parsedPath);

            String bodyFieldPath = Strings.emptyToNull(rule.getBody());

            if(bodyFieldPath != null && !bodyFieldPath.equals("*")) {
                ImmutableList<Descriptors.FieldDescriptor> fieldDescriptor =
                    ProtobufDescriptorJavaUtil.fieldPath(inputDescriptor, bodyFieldPath);

                if(fieldDescriptor.isEmpty()) {
                    List<String> pathSegments = Splitter.on('.').omitEmptyStrings().trimResults().splitToList(path);

                    while(!pathSegments.isEmpty()) {
                        pathSegments.remove(pathSegments.size() - 1);

                        fieldDescriptor = ProtobufDescriptorJavaUtil.fieldPath(inputDescriptor, bodyFieldPath);

                        if(!fieldDescriptor.isEmpty()) {
                            // TODO: remove bodyFieldPath segments until we have a fieldDescriptor
                            List<String> availableFields = Iterables.getLast(fieldDescriptor).getMessageType()
                                                                    .getFields()
                                                                    .stream()
                                                                    .map(Descriptors.FieldDescriptor::getName)
                                                                    .collect(Collectors.toList());
                            throw new IllegalArgumentException("'body' attribute refers to non-existent field " +
                                                               "'" + bodyFieldPath + "'. Available fields: " +
                                                               availableFields);
                        }
                    }
                }
            }

            methodsToGenerate.add(new ResourceMethodToGenerate(
                sam.getMethodDescriptor().getName(),
                method,
                parsedPath.toPath(),
                pathParams,
                bodyFieldPath,
                ProtobufDescriptorJavaUtil.genClassName(inputDescriptor),
                ProtobufDescriptorJavaUtil.genClassName(outputDescriptor),
                methodIndex++
            ));
        }

        return methodsToGenerate.build();
    }

    public static ImmutableList<PathParam> parsePathParams(Descriptors.Descriptor inputDescriptor, PathParser.ParsedPath path) {
        ImmutableList.Builder<PathParam> pathParams = ImmutableList.builder();
        path.visit(new PathParser.EmptySegmentVisitor() {
            @Override
            public void visit(PathParser.NamedVariable namedVariable) {
                ImmutableList<Descriptors.FieldDescriptor> fieldDescriptor =
                    ProtobufDescriptorJavaUtil.fieldPath(inputDescriptor, namedVariable.getName());

                if(fieldDescriptor.isEmpty())
                    throw new IllegalArgumentException("Couldn't find path param: " + namedVariable.getName()
                                                       + " in input type: " + inputDescriptor.toProto());

                Descriptors.FieldDescriptor descriptor = Iterables.getLast(fieldDescriptor);

                if(descriptor.isMapField() || descriptor.isRepeated())
                    throw new IllegalArgumentException(
                        "Cannot map path param '" + namedVariable.getName() + "' as URL mapping is not supported " +
                        "for map or repeated field types."
                    );

                pathParams.add(new PathParam(namedVariable.getName(), fieldDescriptor));
            }
        });

        return pathParams.build();
    }

    @Value
    @Builder
    static class ResourceToGenerate {
        Descriptors.ServiceDescriptor serviceDescriptor;
        String javaPackage;
        String className;
        String grpcStub; // fully-qualified class name;
        List<ResourceMethodToGenerate> methods;
        boolean parseHeaders;
        String fileName;

        String grpcJerseyVersion() {
            return Build.version();
        }

        String sourceProtoFile() {
            return serviceDescriptor.getFile().getName();
        }
    }

    @Value
    static class PathParam {
        String name;
        ImmutableList<Descriptors.FieldDescriptor> fieldDescriptor;

        String nameSanitized() {
            return CharMatcher.javaLetterOrDigit().retainFrom(name);
        }

        List<String> descriptorPath() {
            return fieldDescriptor.stream()
                .map(Descriptors.FieldDescriptor::getName)
                .collect(Collectors.toList());
        }

        String descriptorJoined() {
            return Joiner.on(',').join(descriptorPath());
        }
    }

    @Value
    static class ResourceMethodToGenerate {
        String methodName;
        String method; // GET, POST...
        String path;
        List<PathParam> pathParams;
        String bodyFieldPath;
        String requestType;
        String responseType;
        int methodIndex;

        String methodNameLower() {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName);
        }
    }

    /**
     * Named tuple of (service descriptor, method descriptor proto)
     */
    @Value
    static class ServiceAndMethod {
        Descriptors.ServiceDescriptor serviceDescriptor;
        DescriptorProtos.MethodDescriptorProto methodDescriptor;
    }
}
