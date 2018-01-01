package {{javaPackage}};

import com.fullcontact.rpc.jersey.JerseyUnaryObserver;
import com.fullcontact.rpc.jersey.JerseyStreamingObserver;
import com.fullcontact.rpc.jersey.RequestParser;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.glassfish.jersey.server.ChunkedOutput;

import java.io.IOException;
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
            @Context UriInfo uriInfo
            {{#parseHeaders}}
            ,@Context HttpHeaders headers
            {{/parseHeaders}}
            {{#bodyFieldPath}}
            ,String body
{{/bodyFieldPath}}
            ,@Suspended final AsyncResponse asyncResponse) throws IOException {
        JerseyUnaryObserver<{{responseType}}> observer = new JerseyUnaryObserver<>(asyncResponse);
        {{requestType}}.Builder r = {{requestType}}.newBuilder();
    {{#parseHeaders}}
        // Shadowed to prevent building up headers
        {{grpcStub}} stub;
    {{/parseHeaders}}
        try {
            {{#parseHeaders}}
            stub = RequestParser.parseHeaders(headers, this.stub);
            {{/parseHeaders}}
            {{#bodyFieldPath}}
            RequestParser.handleBody("{{bodyFieldPath}}",r,body);
            {{/bodyFieldPath}}
            {{^bodyFieldPath}}
            RequestParser.parseQueryParams(uriInfo,r);
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
    public ChunkedOutput<String> {{methodName}}_{{method}}_{{methodIndex}}(
            {{#pathParams}}
            @PathParam("{{name}}") String {{nameSanitized}},
            {{/pathParams}}
            @Context UriInfo uriInfo
            {{#parseHeaders}}
            ,@Context HttpHeaders headers
            {{/parseHeaders}}
            ,@Context Request context
            {{#bodyFieldPath}}
            ,String body{{/bodyFieldPath}}) throws IOException {
        final ChunkedOutput<String> output = new ChunkedOutput<String>(String.class);
        Variant variant = context.selectVariant(VARIANT_LIST);
        boolean sse = "text/event-stream".equals(variant.getMediaType().toString());
        JerseyStreamingObserver<{{responseType}}> observer = new JerseyStreamingObserver<>(output, sse);
        {{requestType}}.Builder r = {{requestType}}.newBuilder();
    {{#parseHeaders}}
        // Shadowed to prevent building up headers
        {{grpcStub}} stub;
    {{/parseHeaders}}
        try {
            {{#parseHeaders}}
            stub = RequestParser.parseHeaders(headers, this.stub);
            {{/parseHeaders}}
            {{#bodyFieldPath}}
            RequestParser.handleBody("{{bodyFieldPath}}",r,body);
            {{/bodyFieldPath}}
            {{^bodyFieldPath}}
            RequestParser.parseQueryParams(uriInfo,r);
            {{/bodyFieldPath}}
            {{#pathParams}}
            RequestParser.setFieldSafely(r, "{{name}}", {{nameSanitized}});
            {{/pathParams}}
        } catch(Exception e) {
            observer.onError(e);
            return output;
        }

        stub.{{methodNameLower}}(r.build(), observer);

        return output;
    }
    {{/streamMethods}}
}
