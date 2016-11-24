package com.fullcontact.rpc.jersey;

import com.google.common.collect.Lists;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.glassfish.jersey.uri.internal.UriTemplateParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.runners.Parameterized.Parameters;

/**
 * Tests for {@link PathParser}
 *
 * @author Michael Rose (xorlev)
 */
@RunWith(Parameterized.class)
public class PathParserTest {
    private final TestCase testCase;

    public PathParserTest(TestCase testCase) {
        this.testCase = testCase;
    }

    @Test
    public void testParse() throws Exception {
        try {
            PathParser.ParsedPath parsed = PathParser.parse(testCase.path);

            assertEquals(testCase.expectedPath, parsed);

            try {
                assertEquals(testCase.generatedPath.orElse("No generated URL expected"), parsed.toPath());
            } catch(IllegalArgumentException e) {
                testCase.generatedPath.ifPresent(s -> fail("Expected URL but IAE generated: " + e.getMessage()));
            }
        } catch(PathParser.ParseException e) {
            testCase.generatedPath.ifPresent(s -> fail(e.getMessage()));
        }
    }

    @Test
    public void testJerseyParse() throws Exception {
        if(testCase.generatedPath.isPresent()) {
            PathParser.ParsedPath parsed = PathParser.parse(testCase.path);
            UriTemplateParser parser = new UriTemplateParser(parsed.toPath());
        }

    }

    @Parameters(name = "({index}) {0}")
    public static Collection<TestCase> data() {
        List<TestCase> testCases = Lists.newArrayList(
            new TestCase("/resource/{user_id}/{path=hello/{person}}/*/test",
                         null
                         // no url generated: nested is invalid
            ),
            new TestCase("/resource/{user_id}/{path=**}/*/test",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("resource"),
                             new PathParser.NamedVariable("user_id"),
                             new PathParser.NamedVariable("path", PathParser.GreedyWildcard.INSTANCE),
                             PathParser.Wildcard.INSTANCE,
                             new PathParser.Literal("test")),
                         "/resource/{user_id}/{path: .+}/{1: [^/]+}/test"

            ),
            new TestCase("/resource/{test.nested.user_id}/{path=**}/*/test",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("resource"),
                             new PathParser.NamedVariable("test.nested.user_id"),
                             new PathParser.NamedVariable("path", PathParser.GreedyWildcard.INSTANCE),
                             PathParser.Wildcard.INSTANCE,
                             new PathParser.Literal("test")),
                         "/resource/{test.nested.user_id}/{path: .+}/{1: [^/]+}/test"

            ),
            new TestCase("/resource/{test.nested.user_id}/{path=hello/**}/*/test",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("resource"),
                             new PathParser.NamedVariable("test.nested.user_id"),
                             new PathParser.NamedVariable("path", new PathParser.Literal("hello"), PathParser.GreedyWildcard.INSTANCE),
                             PathParser.Wildcard.INSTANCE,
                             new PathParser.Literal("test")),
                         "/resource/{test.nested.user_id}/{path: hello/.+}/{1: [^/]+}/test"

            ),
            new TestCase("/ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ/{test.nested.user_id}/{path=hello/**}/*/test",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ"),
                             new PathParser.NamedVariable("test.nested.user_id"),
                             new PathParser.NamedVariable("path", new PathParser.Literal("hello"), PathParser.GreedyWildcard.INSTANCE),
                             PathParser.Wildcard.INSTANCE,
                             new PathParser.Literal("test")),
                         "/ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ/{test.nested.user_id}/{path: hello/.+}/{1: [^/]+}/test"

            ),
            new TestCase("/Приют/{test.nested.user_id}/{path=hello/**}/*/test",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("Приют"),
                             new PathParser.NamedVariable("test.nested.user_id"),
                             new PathParser.NamedVariable("path", new PathParser.Literal("hello"), PathParser.GreedyWildcard.INSTANCE),
                             PathParser.Wildcard.INSTANCE,
                             new PathParser.Literal("test")),
                         "/Приют/{test.nested.user_id}/{path: hello/.+}/{1: [^/]+}/test"

            ),
            new TestCase("/resource/{user_id}/**/*/test",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("resource"),
                             new PathParser.NamedVariable("user_id"),
                             PathParser.GreedyWildcard.INSTANCE,
                             PathParser.Wildcard.INSTANCE,
                             new PathParser.Literal("test")),
                         "/resource/{user_id}/{1: .+}/{2: [^/]+}/test"

            ),
            new TestCase("/resource/{user_id}",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("resource"),
                             new PathParser.NamedVariable("user_id")),
                         "/resource/{user_id}"

            ),
            new TestCase("/resource/{user_id}/",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("resource"),
                             new PathParser.NamedVariable("user_id")),
                         "/resource/{user_id}"

            ),
            new TestCase("/resource/user_id}/",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("resource"),
                             new PathParser.NamedVariable("user_id"))
                         // missing '{'

            ),
            new TestCase("/resource/{user_id/",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("resource"),
                             new PathParser.NamedVariable("user_id"))
                         // missing '}'

            ),
            new TestCase("/resource",
                         new PathParser.ParsedPath(
                             new PathParser.Literal("resource")),
                         "/resource"

            ),
            new TestCase("/",
                         new PathParser.ParsedPath(),
                         "/"

            )
        );

        return testCases;
    }

    @EqualsAndHashCode
    @ToString
    static class TestCase {
        private final String path;
        private final PathParser.ParsedPath expectedPath;
        private final Optional<String> generatedPath;

        public TestCase(String path, PathParser.ParsedPath expectedPath) {
            this(path, expectedPath, null);
        }

        public TestCase(String path, PathParser.ParsedPath expectedPath, String generatedPath) {
            this.path = path;
            this.expectedPath = expectedPath;
            this.generatedPath = Optional.ofNullable(generatedPath);
        }
    }
}
