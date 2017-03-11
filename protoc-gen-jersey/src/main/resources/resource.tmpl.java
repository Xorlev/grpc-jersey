package {{javaPackage}};

import com.fullcontact.rpc.jersey.JerseyStreamObserver;
import com.fullcontact.rpc.jersey.RequestParser;
import com.fullcontact.rpc.jersey.GrpcErrorUtil;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

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
    {{#methods}}

    @{{method}}
    @Path("{{path}}")
    public void {{methodName}}_{{method}}_{{methodIndex}}(
            {{#pathParams}}
            @PathParam("{{name}}") String {{nameSanitized}},
            {{/pathParams}}
            @Context UriInfo uriInfo,
            {{#parseHeaders}}
            @Context HttpHeaders headers,
            {{/parseHeaders}}
            {{#bodyFieldPath}}
            String body,
{{/bodyFieldPath}}
            @Suspended final AsyncResponse asyncResponse) throws IOException {
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
            {{#bodyFieldPath}}
        } catch(InvalidProtocolBufferException e) {
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).build());
            return;

            {{/bodyFieldPath}}
        } catch(Exception e) {
            asyncResponse.resume(GrpcErrorUtil.createJerseyResponse(e));
            return;
        }
        stub.{{methodNameLower}}(r.build(), new JerseyStreamObserver<>(asyncResponse));
    }
    {{/methods}}
}
