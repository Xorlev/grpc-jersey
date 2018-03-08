package com.fullcontact.rpc.jersey;

import com.google.api.AnnotationsProto;
import com.google.common.io.ByteStreams;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.compiler.PluginProtos;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Generates Jersey REST->RPC resource code
 *
 * @author Michael Rose (xorlev)
 */
public class Main {
    public static void main(String[] args) throws IOException, Descriptors.DescriptorValidationException {
        InputStream is = System.in;

        if (args.length > 0) {
            File replayFile = new File(args[0]);
            FileOutputStream fos = new FileOutputStream(replayFile);

            ByteStreams.copy(System.in, fos);
            fos.close();

            is = new FileInputStream(replayFile);
        }

        ExtensionRegistryLite registryLite = ExtensionRegistryLite.newInstance();
        AnnotationsProto.registerAllExtensions(registryLite);

        CodeGenerator codeGenerator = new CodeGenerator();
        PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(is, registryLite);
        PluginProtos.CodeGeneratorResponse response = codeGenerator.generate(request);

        response.writeTo(System.out);
    }

}
