package com.fullcontact.rpc.jersey;

import com.fullcontact.rpc.TestRequest;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test for {@link CodeGenerator}
 *
 * @author Michael Rose (xorlev)
 */
public class CodeGeneratorTest {
    @Test
    public void parsePathParams() throws Exception {
        assertThat(CodeGenerator.parsePathParams(TestRequest.getDescriptor(), "/users/{s}/{uint3}/{nt.f1}"))
            .extracting("name")
            .isEqualTo(Lists.newArrayList("s", "uint3", "nt.f1"));
    }

}
