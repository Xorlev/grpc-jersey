package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.TestRequest;

import com.google.common.collect.Iterables;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CodeGenerator}
 *
 * @author Michael Rose (xorlev)
 */
public class CodeGeneratorTest {
    @Test
    public void parsePathParams() throws Exception {
        assertThat(CodeGenerator.parsePathParams(TestRequest.getDescriptor(), PathParser.parse("/users/{s}/{uint3}/{nt.f1}")))
            .extracting("name")
            .containsExactly("s", "uint3", "nt.f1");
        assertThat(CodeGenerator.parsePathParams(TestRequest.getDescriptor(), PathParser.parse("/users/{s}/{uint3}/{nt.f1}")))
            .extracting(x -> Iterables.getLast(x.getFieldDescriptor()).getName())
            .containsExactly("s", "uint3", "f1");
    }

}
