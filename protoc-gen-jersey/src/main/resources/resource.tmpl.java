package {{javaPackage}};

import com.fullcontact.rpc.jersey.HttpHeaderInterceptors;
import com.fullcontact.rpc.jersey.JerseyUnaryObserver;
import com.fullcontact.rpc.jersey.JerseyStreamingObserver;
import com.fullcontact.rpc.jersey.RequestParser;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.io.OutputStream;
import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import static com.fullcontact.rpc.jersey.JerseyStreamingObserver.VARIANT_LIST;

@javax.annotation.Generated(
    value = "by grpc-jersey compiler (version {{grpcJerseyVersion}})",
    comments = "Source: {{sourceProtoFile}}")
@Produces({"application/json; charset=UTF-8"})
@Consumes({"application/json; charset=UTF-8"})
@Path("/")
public class {{className}} {
    private {{grpcStub}} stub;

    public {{className}}({{grpcStub}} stub) {
        this.stub = stub;
    }
    {{#unaryMethods}}

    @{{method}}
    @Path("{{path}}")
    public void {{methodName}}_{{method}}_{{methodIndex}}(
            {{#pathParams}}
            @PathParam("{{name}}") String {{nameSanitized}},
            {{/pathParams}}
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers
            {{#bodyFieldPath}}
            ,String body
{{/bodyFieldPath}}
            ,@Suspended final AsyncResponse asyncResponse) throws IOException {
        HttpHeaderInterceptors.HttpHeaderClientInterceptor interceptor =
            HttpHeaderInterceptors.clientInterceptor(headers);
        JerseyUnaryObserver<{{responseType}}> observer = new JerseyUnaryObserver<>(asyncResponse, interceptor);
        {{requestType}}.Builder r = {{requestType}}.newBuilder();
        {{grpcStub}} stub = this.stub;
        try {
            {{#isProxy}}
            stub = RequestParser.parseHeaders(headers, stub);
            stub = stub.withInterceptors(interceptor);
            {{/isProxy}}
            {{#bodyFieldPath}}
            RequestParser.handleBody("{{bodyFieldPath}}", r, body);
            {{/bodyFieldPath}}
            {{^bodyFieldPath}}
            RequestParser.parseQueryParams(uriInfo, r);
            {{/bodyFieldPath}}
            {{#pathParams}}
            RequestParser.setFieldSafely(r, "{{name}}", {{nameSanitized}});
            {{/pathParams}}
        } catch(Exception e) {
            observer.onError(e);
            return;
        }
        stub.{{methodNameLower}}(r.build(), observer);
    }
    {{/unaryMethods}}
    {{#streamMethods}}

    @{{method}}
    @Path("{{path}}")
    @Produces({"application/json; charset=utf-8", "text/event-stream; charset=utf-8"})
    public void {{methodName}}_{{method}}_{{methodIndex}}(
            {{#pathParams}}
            @PathParam("{{name}}") String {{nameSanitized}},
            {{/pathParams}}
            @Suspended final AsyncResponse asyncResponse,
            @Context HttpServletRequest servletRequest,
            @Context HttpServletResponse servletResponse,
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers,
            @Context Request context
            {{#bodyFieldPath}}
            ,String body{{/bodyFieldPath}}) throws IOException {
        Variant variant = context.selectVariant(VARIANT_LIST);
        boolean sse = "text/event-stream".equals(variant.getMediaType().toString());

        HttpHeaderInterceptors.HttpHeaderClientInterceptor interceptor =
            HttpHeaderInterceptors.clientInterceptor(headers);
        JerseyStreamingObserver<{{responseType}}> observer =
            new JerseyStreamingObserver<>(interceptor, servletRequest, servletResponse, sse);
        {{requestType}}.Builder r = {{requestType}}.newBuilder();
        {{grpcStub}} stub = this.stub;
        try {
            {{#isProxy}}
            stub = RequestParser.parseHeaders(headers, stub);
            stub = stub.withInterceptors(interceptor);
            {{/isProxy}}
            {{#bodyFieldPath}}
            RequestParser.handleBody("{{bodyFieldPath}}", r, body);
            {{/bodyFieldPath}}
            {{^bodyFieldPath}}
            RequestParser.parseQueryParams(uriInfo, r);
            {{/bodyFieldPath}}
            {{#pathParams}}
            RequestParser.setFieldSafely(r, "{{name}}", {{nameSanitized}});
            {{/pathParams}}
        } catch(Exception e) {
            observer.onError(e);
            return;
        }

        stub.{{methodNameLower}}(r.build(), observer);
    }
    {{/streamMethods}}
}
