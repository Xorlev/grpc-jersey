package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.jersey.util.ProtobufDescriptorJavaUtil;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.compiler.PluginProtos;
import lombok.Value;


import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        Boolean isProxy = ("proxy".equals(request.getParameter()));
        Map<String, Descriptors.Descriptor> lookup = new HashMap<>();
        PluginProtos.CodeGeneratorResponse.Builder response = PluginProtos.CodeGeneratorResponse.newBuilder();

        List<Descriptors.FileDescriptor> fds = Lists.newArrayList(
            DescriptorProtos.MethodOptions.getDescriptor().getFile(),
            AnnotationsProto.getDescriptor(),
            HttpRule.getDescriptor().getFile()
        );
        for(DescriptorProtos.FileDescriptorProto p : request.getProtoFileList()) {
            // Descriptors are provided in dependency-topological order
            // each time we collect a new FileDescriptor, we add it to a
            // mutable list of descriptors and append the entire dependency
            // chain to each new FileDescriptor to allow crossLink() to function.
            // TODO(xorlev) might have to be more selective about deps in future
            Descriptors.FileDescriptor fd = Descriptors.FileDescriptor.buildFrom(
                p, fds.toArray(new Descriptors.FileDescriptor[] {})
            );
            fds.add(fd);

            // if type starts with a ".", it's in this package
            // otherwise it's fully qualified
            String protoPackage = p.getPackage();
            for(DescriptorProtos.DescriptorProto d : p.getMessageTypeList()) {
                lookup.put("." + protoPackage+"."+d.getName(), fd.findMessageTypeByName(d.getName()));

            }

            // Find RPC methods with HTTP extensions
            List<ServiceAndMethod> toGenerate = new ArrayList<>();
            for(DescriptorProtos.ServiceDescriptorProto s : p.getServiceList()) {
                for(DescriptorProtos.MethodDescriptorProto m : s.getMethodList()) {
                    if(m.getOptions().hasExtension(AnnotationsProto.http)) {
                        // TODO(xorlev): support server streaming
                        if(m.getServerStreaming() || m.getClientStreaming())
                            throw new IllegalArgumentException("http annotations cannot be used with streaming methods");

                        toGenerate.add(new ServiceAndMethod(s, m));
                    }
                }
            }

            if(!toGenerate.isEmpty())
                generateResource(response, lookup, p, toGenerate, isProxy);
        }

        return response.build();
    }

    private void generateResource(
            PluginProtos.CodeGeneratorResponse.Builder response,
            Map<String, Descriptors.Descriptor> lookup,
            DescriptorProtos.FileDescriptorProto p,
            List<ServiceAndMethod> generate,
            Boolean isProxy) {
        DescriptorProtos.ServiceDescriptorProto serviceDescriptor = generate.get(0).getServiceDescriptor();
        String packageName = ProtobufDescriptorJavaUtil.javaPackage(p);
        String className = ProtobufDescriptorJavaUtil.jerseyResourceClassName(serviceDescriptor);
        String grpcImplClass = (isProxy)?
            ProtobufDescriptorJavaUtil.grpcStubClass(p, serviceDescriptor):
            ProtobufDescriptorJavaUtil.grpcImplBaseClass(p, serviceDescriptor);
        String fileName = packageName.replace('.', '/') + "/" + className + ".java";

        ImmutableList.Builder<ResourceMethodToGenerate> methods = ImmutableList.builder();
        for(ServiceAndMethod sam : generate) {
            Descriptors.Descriptor inputDescriptor = lookup.get(sam.getMethodDescriptor().getInputType());
            Descriptors.Descriptor outputDescriptor = lookup.get(sam.getMethodDescriptor().getOutputType());
            List<ResourceMethodToGenerate> methodToGenerate = parseRule(sam, inputDescriptor, outputDescriptor);
            methods.addAll(methodToGenerate);
        }

        ResourceToGenerate r = new ResourceToGenerate(
            ProtobufDescriptorJavaUtil.javaPackage(p),
            className,
            grpcImplClass,
            methods.build(),
            isProxy
        );

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("resource.tmpl.java");
        StringWriter writer = new StringWriter();
        mustache.execute(writer, r);

        response.addFile(PluginProtos.CodeGeneratorResponse.File.newBuilder()
                         .setContent(writer.toString())
                         .setName(fileName)
                         .build());

        System.err.println(writer.toString());
    }

    private ImmutableList<ResourceMethodToGenerate> parseRule(ServiceAndMethod sam,
                                                              Descriptors.Descriptor inputDescriptor,
                                                              Descriptors.Descriptor outputDescriptor) {
        HttpRule baseRule = sam.getMethodDescriptor().getOptions().getExtension(AnnotationsProto.http);

        ImmutableList<HttpRule> rules = ImmutableList.<HttpRule>builder()
            .add(baseRule)
            .addAll(baseRule.getAdditionalBindingsList())
            .build();

        ImmutableList.Builder<ResourceMethodToGenerate> methodsToGenerate = ImmutableList.builder();
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

            ImmutableList<PathParam> pathParams = parsePathParams(inputDescriptor, path);

            // TODO(xorlev): check for URL overlap
            // TODO(xorlev): handle full paths
            String bodyFieldPath = Strings.emptyToNull(rule.getBody());

            if(bodyFieldPath != null && !bodyFieldPath.equals("*")) {
                ImmutableList<Descriptors.FieldDescriptor> fieldDescriptor = ProtobufDescriptorJavaUtil
                    .fieldPath(inputDescriptor, bodyFieldPath);
                if(fieldDescriptor.isEmpty()) {
                    List<String> availableFields = inputDescriptor.getFields()
                        .stream()
                        .map(Descriptors.FieldDescriptor::getName)
                        .collect(Collectors.toList());
                    throw new IllegalArgumentException("'body' attribute refers to non-existent field " +
                                                       "'" + bodyFieldPath + "'. Available fields: " + availableFields);
                }
            }


            methodsToGenerate.add(new ResourceMethodToGenerate(
                sam.getMethodDescriptor().getName(),
                method,
                path,
                pathParams,
                bodyFieldPath,
                ProtobufDescriptorJavaUtil.genClassName(inputDescriptor),
                ProtobufDescriptorJavaUtil.genClassName(outputDescriptor)
            ));
        }

        return methodsToGenerate.build();
    }

    public static ImmutableList<PathParam> parsePathParams(Descriptors.Descriptor inputDescriptor, String path) {
        // TODO: handle unnamed wildcards & multi-level paths
        //     Template = "/" Segments [ Verb ] ;
        //     Segments = Segment { "/" Segment } ;
        //     Segment  = "*" | "**" | LITERAL | Variable ;
        //     Variable = "{" FieldPath [ "=" Segments ] "}" ;
        //     FieldPath = IDENT { "." IDENT } ;
        //     Verb     = ":" LITERAL ;
        // If we have wildcards, we can emit regex instead

        Pattern pattern = Pattern.compile(".*\\{([a-z_\\.]+)\\}.*");
        Matcher m = pattern.matcher(path);
        ImmutableList.Builder<PathParam> pathParams = ImmutableList.builder();
        while(m.find()) {
            String name = m.group(1);
            ImmutableList<Descriptors.FieldDescriptor> fieldDescriptor =
                ProtobufDescriptorJavaUtil.fieldPath(inputDescriptor, name);

            if(fieldDescriptor.isEmpty())
                throw new IllegalArgumentException("Couldn't find path param: " + name
                                                   + " in input type: " + inputDescriptor.toProto());

            pathParams.add(new PathParam(name, fieldDescriptor));
        }

        return pathParams.build();
    }

    @Value
    static class ResourceToGenerate {
        String javaPackage;
        String className;
        String grpcStub; // fully-qualified class name;
        List<ResourceMethodToGenerate> methods;
        Boolean parseHeaders;
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

        String methodNameLower() {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName);
        }
    }

    @Value
    static class ServiceAndMethod {
        DescriptorProtos.ServiceDescriptorProto serviceDescriptor;
        DescriptorProtos.MethodDescriptorProto methodDescriptor;
    }
}
