package com.fullcontact.rpc.jersey;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parser for google.api.http path language,
 * based on RFC 6570(https://tools.ietf.org/html/rfc6570) Section 3.2.3 Reserved Expansion.
 *
 * Template = "/" Segments [ Verb ] ;
 * Segments = Segment { "/" Segment } ;
 * Segment  = "*" | "**" | LITERAL | Variable ;
 * Variable = "{" FieldPath [ "=" Segments ] "}" ;
 * FieldPath = IDENT { "." IDENT } ;
 * Verb     = ":" LITERAL ;
 *
 * Parser supports all but the "Verb" clause which appears to be superfluous, and disallows nested variable segments.
 *
 * Parsed template trees also emit Jersey @PATH compatible paths with invocation of
 * {@link ParsedPath#toPath()}
 *
 * @author Michael Rose (xorlev)
 */
public class PathParser {
    /**
     * Parse google.api.http path template into a {@link ParsedPath}
     */
    public static ParsedPath parse(String path) {
        Parser parser = new Parser(path);

        List<Segment> segments = new ArrayList<>();
        while(parser.hasNext()) {
            Segment segment = parseSegment(parser);

            if(segment != null)
                segments.add(segment);
        }

        return new ParsedPath(segments);
    }

    private static Segment parseSegment(Parser parser) {
        // '/' is our segment separator
        if(parser.peek() == '/')
            parser.next();

        if(!parser.hasNext())
            return null;

        switch(parser.peek()) {
            case '{':
                return parseNamedVariable(parser);
            case '*':
                return parseWildcard(parser);
            default:
                return parseLiteral(parser);
        }
    }

    private static NamedVariable parseNamedVariable(Parser parser) {
        parser.next(); // consume '{'
        String name = parser.consumeToSeparator();
        List<Segment> segments = new ArrayList<>();
        if(parser.peek() == '=') {
            parser.next();
            segments.add(parseSegment(parser));

            // Parse nested segments if available
            while(parser.peek() == '/') {
                parser.next();
                Segment segment = parseSegment(parser);

                if(segment instanceof NamedVariable)
                    throw new ParseException("Variables cannot be nested.");

                segments.add(segment);
            }
        }

        if(parser.peek() == '}') {
            parser.next();
        } else {
            parser.throwException("Expected '}', found: " + parser.peek());
        }

        return new NamedVariable(name, ImmutableList.copyOf(segments));
    }

    private static Segment parseWildcard(Parser parser) {
        parser.next();
        if(parser.peek() == '*') {
            parser.next();
            return GreedyWildcard.INSTANCE;
        } else {
            return Wildcard.INSTANCE;
        }
    }

    private static Literal parseLiteral(Parser parser) {
        if(parser.peek() == '/')
            parser.next();

        if(parser.isSeparator())
            parser.throwException("Expected literal, found separator: " + parser.peek());

        String literal = parser.consumeToSeparator();

        if(parser.hasNext() && parser.peek() != '/')
            parser.throwException("Expected '/', found: " + parser.peek());

        return new Literal(literal);
    }

    /**
     * Parse state container + helpful utility methods
     */
    static class Parser {
        private static final CharMatcher SEPARATORS = CharMatcher.anyOf("{}/=");

        private char[] buffer;
        private int position;

        public Parser(String buffer) {
            this.buffer = buffer.toCharArray();
        }

        public int position() {
            return position;
        }

        public boolean hasNext() {
            return position < buffer.length;
        }

        public char peek() {
            return buffer[position];
        }

        public char next() {
            if(!hasNext())
                throw new ParseException("Buffer overflow, tried to call next() with no remaining characters");

            return buffer[position++];
        }

        public boolean isSeparator() {
            return SEPARATORS.matches(peek());
        }

        public String consumeToSeparator() {
            StringBuilder sb = new StringBuilder();
            while(hasNext() && !isSeparator()) {
                sb.append(next());
            }

            return sb.toString();
        }

        public void throwException(String message) {
            String path = new String(buffer);

            throw new ParseException(
                "Unexpected character: " + peek() + " at position " + position
                + " for in: " + path + (message.isEmpty() ? "." : ". " + message)
            );
        }
    }

    /**
     * Container for a parsed template tree
     */
    @Value
    public static class ParsedPath {
        ImmutableList<Segment> segments;

        public ParsedPath(Segment... segments) {
            this(ImmutableList.copyOf(segments));
        }

        public ParsedPath(Collection<Segment> segments) {
            this.segments = ImmutableList.copyOf(segments);
        }

        /**
         * Visit all top-level segments in ParsedPath. Does not handle nested segments.
         *
         * @return visitor provided
         */
        public <T extends SegmentVisitor> T visit(T visitor) {
            for(Segment segment : segments)
                segment.accept(visitor);

            return visitor;
        }

        /**
         * Generates Jersey @PATH compatible path
         * @throws IllegalArgumentException if parsed path will not correctly map to a Jersey path, e.x. nested named
         * variables
         */
        public String toPath() {
            return visit(new JerseyPathSegmentVisitor()).toPath();
        }
    }

    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    public interface SegmentVisitor {
        void visit(Literal literal);
        void visit(NamedVariable namedVariable);
        void visit(GreedyWildcard greedyWildcard);
        void visit(Wildcard wildcard);
    }

    public static class JerseyPathSegmentVisitor implements SegmentVisitor {
        private List<String> pathSegments = new ArrayList<>();
        private int anonymousParam = 0;

        @Override
        public void visit(Literal literal) {
            pathSegments.add(literal.toPath());
        }

        @Override
        public void visit(NamedVariable namedVariable) {
            pathSegments.add(namedVariable.toPath());
        }

        @Override
        public void visit(GreedyWildcard greedyWildcard) {
            pathSegments.add("{" + ++anonymousParam + ": " + greedyWildcard.toPath() + "}");
        }

        @Override
        public void visit(Wildcard wildcard) {
            pathSegments.add("{" + ++anonymousParam + ": " + wildcard.toPath() + "}");
        }

        public String toPath() {
            return "/" + Joiner.on('/').join(pathSegments);
        }
    }

    public abstract static class EmptySegmentVisitor implements SegmentVisitor {
        @Override
        public void visit(Literal literal) {}
        @Override
        public void visit(NamedVariable namedVariable) {}
        @Override
        public void visit(GreedyWildcard greedyWildcard) {}
        @Override
        public void visit(Wildcard wildcard) {}
    }

    @ToString
    abstract static class Segment {
        public abstract void accept(SegmentVisitor visitor);
        public abstract String toPath();
    }

    @Value
    @EqualsAndHashCode(callSuper=false)
    public static class Literal extends Segment {
        String literal;

        @Override
        public void accept(SegmentVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toPath() {
            return literal;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper=false)
    public static class GreedyWildcard extends Segment {
        public static GreedyWildcard INSTANCE = new GreedyWildcard();

        private GreedyWildcard() {}

        @Override
        public void accept(SegmentVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toPath() {
            return ".+";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper=false)
    public static class Wildcard extends Segment {
        public static Wildcard INSTANCE = new Wildcard();

        private Wildcard() {}

        @Override
        public void accept(SegmentVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toPath() {
            return "[^/]+";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper=false)
    public static class NamedVariable extends Segment {
        @NonNull String name;
        @NonNull ImmutableList<Segment> segments;

        public NamedVariable(String name) {
            this(name, Collections.emptyList());
        }

        public NamedVariable(String name, Segment... segments) {
            this(name, ImmutableList.copyOf(segments));
        }

        public NamedVariable(String name, Collection<Segment> segments) {
            this.name = name;
            this.segments = ImmutableList.copyOf(segments);
        }

        @Override
        public void accept(SegmentVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toPath() {
            if(segments.isEmpty()) {
                return "{" + name + "}";
            } else {
                if(segments.stream().anyMatch(s -> s instanceof NamedVariable)) {
                    throw new IllegalArgumentException(
                        "Jersey @PATH does not support nested named variables: " + this);
                }

                return "{" + name + ": " + segments.stream().map(Segment::toPath).collect(Collectors.joining("/")) + "}";
            }
        }
    }
}
