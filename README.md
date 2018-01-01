# grpc-jersey [![Build Status](https://travis-ci.org/Xorlev/grpc-jersey.svg?branch=master)](https://travis-ci.org/Xorlev/grpc-jersey) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.xorlev.grpc-jersey/protoc-gen-jersey/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.xorlev.grpc-jersey/protoc-gen-jersey)
protoc plugin for compiling [gRPC](https://www.grpc.io/) RPC services as Jersey/REST endpoints. Uses the
[HttpRule](https://cloud.google.com/service-management/reference/rpc/google.api#http) annotations also
used by the [grpc-gateway](https://github.com/grpc-ecosystem/grpc-gateway) project to drive resource generation.

## Example Usage

grpc-jersey requires a minimum of Java 8 at this time.

Snapshot artifacts are available on the Sonatype snapshots repository, releases are available on Maven Central.

Example provided here uses the [gradle-protobuf-plugin](https://github.com/google/protobuf-gradle-plugin)
but an example using Maven can be found [in examples](https://github.com/Xorlev/grpc-jersey/tree/master/examples/maven/pom.xml).

```groovy
ext {
    protobufVersion = "3.2.0"
    grpcVersion = "1.2.0"
    grpcJerseyVersion = "0.1.4"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
    plugins {
        grpc {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
        }
        jersey {
            artifact = "com.xorlev.grpc-jersey:protoc-gen-jersey:${grpcJerseyVersion}"
        }
    }
    generateProtoTasks {
        all()*.plugins {
            grpc {}
            jersey {}
        }
    }
}
```

You'll also have to be sure to include the `jersey-rpc-support` package in your service:

```groovy
compile "com.xorlev.grpc-jersey:jersey-rpc-support:${grpcJerseyVersion}"
```

Running `./gradlew build` and a protobuf definition that looks roughly like the below

```proto
syntax = "proto3";

option java_package = "com.fullcontact.rpc.example";
import "google/api/annotations.proto";

service TestService {
    rpc TestMethod (TestRequest) returns (TestResponse) {
        option (google.api.http).get = "/users/{id}";
    }
    rpc TestMethod2 (TestRequest) returns (TestResponse) {
        option (google.api.http) = {
            post: "/users/",
            body: "*";
        };
    }
    rpc StreamMethod1 (TestRequest) returns (stream TestResponse) {
            option (google.api.http).get = "/stream/{s}";
    }
}
message TestRequest {
    string id = 1;
}
message TestResponse {
    string f1 = 1;
}

```

Would compile into a single Jersey resource with one GET handler and one POST handler.

Rules can also be defined in a .yml file. 

```yaml
http:
  rules:
  - selector: TestService.TestMethod4
    get: /users/{id}
  - selector: TestService.TestMethod5
    get: /yaml_users/{s=hello/**}/x/{uint3}/{nt.f1}/*/**/test
  - selector: TestService.TestMethod6
    post: /users/
    body: "*"
    additionalBindings:
      - post: /yaml_users_nested
        body: "nt"
```
Rules defined this way must correspond to methods in the .proto files,
and will overwrite any http rules defined in the proto. The path to your .yml file should be passed in as an option:
```groovy
generateProtoTasks {
    all()*.plugins {
        grpc {}
        jersey {
            option 'yaml=integration-test-base/src/test/proto/http_api_config.yml'
        }
    }
}
```
or 
```xml
    <configuration>
      <pluginId>grpc-jersey</pluginId>
      <pluginArtifact>com.xorlev.grpc-jersey:protoc-gen-jersey:0.1.4:exe:${os.detected.classifier}</pluginArtifact>
      <pluginParameter>yaml=integration-test-base/src/test/proto/http_api_config.yml</pluginParameter>
    </configuration>

```

## Operation modes

grpc-jersey can operate in two different modes: direct invocation on service `ImplBase` or proxy via a client `Stub`.
There are advantages and disadvantages to both, however the primary benefit to the client stub proxy is that RPCs pass
through the same `ServerInterceptor` stack. It's recommended that the client stub passed into the Jersey resource
uses a `InProcessTransport` if living in the same JVM as the gRPC server. A normal grpc-netty channel can be used
for a more traditional reverse proxy.

The proxy stub mode is the default as of 0.2.0.

You can toggle the "direct invocation" mode by passing an option to the grpc-jersey compiler:

```groovy
generateProtoTasks {
    all()*.plugins {
        grpc {}
        jersey {
            option 'direct'
        }
    }
}
```

If you plan to run "dual stack", that is, services serving traffic over both HTTP and RPC, you can configure your
service to share resources and the same interceptor stack and avoid TCP/serialization overhead using a mixture
of in-process transport. The example below uses Dropwizard, but should be adaptable to any Jersey glue you like.

```java
// Shared executor for both services.
Executor executor = Executors.newFixedThreadPool(config.rpcThreads,
        new ThreadFactoryBuilder().setNameFormat("grpc-executor-%d").build());

// Service stack. This is where you define your interceptors.
ServerServiceDefinition serviceStack = ServerInterceptors.intercept(
        new EchoTestService(),
        new GrpcLoggingInterceptor(),
        new MyAuthenticationInterceptor() // your interceptor stack.
);

// External RPC service.
Server externalService = NettyServerBuilder
        .forPort(config.rpcPort)
        .executor(executor)
        .addService(serviceStack)
        .build()
        .start();

// In-memory RPC service for grpc-jersey. This avoids serialization/TCP overheads while still sharing the
// same executor and service stack.
Server internalService = InProcessServerBuilder
        .forName("TestService")
        .executor(executor)
        .addService(serviceStack)
        .build()
        .start();

// In-process stub used by the generated Jersey resource.
TestServiceGrpc.TestServiceStub stub =
        TestServiceGrpc.newStub(InProcessChannelBuilder
                .forName("TestService")
                .usePlaintext(true)
                .directExecutor()
                .build());

// Dropwizard-specific: register shutdown handlers and the Jersey resource.

environment.lifecycle().manage(new Managed() {
    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
        externalService.shutdown();
        internalService.shutdown();
    }
});

environment.jersey().register(new TestServiceGrpcJerseyResource(stub));
```

You can find a complete example of each in the `integration-test-proxy` and `integration-test-serverstub` projects.

## Error handling

grpc-jersey will translate errors raised inside your RPC handler. However, there is some nuance with regards to using
the "proxy" mode or the "direct invocation" mode. If you use the direct invocation on service implementation, uncaught
exceptions will use the default Jersey error handler. If you use the proxy mode, uncaught exceptions will be translated
as INTERNAL errors.

### Error Translation

Errors are translated into a google.rpc.Status message, which has the format when translated to JSON:

```json
{
   "code":15,
   "message":"HTTP 500 (gRPC: DATA_LOSS): Fail-fast: Grue found in write-path.",
   "details":[

   ]
}
```

This payload will be returned in lieu of the actual return type.

The translation of gRPC error code to HTTP error code is done on a best-effort basis:

gRPC Error Name | gRPC Error Code | HTTP Status Code
--- | --- | ---
OK | 0 | 200
CANCELLED | 1 | 503
UNKNOWN | 2 | 500
INVALID_ARGUMENT | 3 | 400
DEADLINE_EXCEEDED | 4 | 503
NOT_FOUND | 5 | 404
ALREADY_EXISTS | 6 | 409
PERMISSION_DENIED | 7 | 403
RESOURCE_EXHAUSTED | 8 | 503
FAILED_PRECONDITION | 9 | 412
ABORTED | 10 | 500
OUT_OF_RANGE | 11 | 416
UNIMPLEMENTED | 12 | 501
INTERNAL | 13 | 500
UNAVAILABLE | 14 | 503
DATA_LOSS | 15 | 500
UNAUTHENTICATED | 16 | 401

The HTTP status code will be applied to the outgoing response as the status code, but will also be a part of the
message as seen above. The rest of the message comes from the gRPC StatusException/StatusRuntimeException description
set by `withDescription(String)`. Augmenting the description will append newlines which will be escaped in the final
output.

The `details` section will always be empty. Protobuf JsonFormat does not support serializing the `google.protobuf.Any`
type. Any details provided will be stripped by the error handling.

#### Retry-After

If `google.rpc.RetryInfo` is provided in the `details` section, this will be translated into a `Retry-After` header,
e.x:

```java
Metadata metadata = new Metadata();
metadata.put(GrpcErrorUtil.RETRY_INFO_KEY,
        RetryInfo.newBuilder().setRetryDelay(Durations.fromSeconds(30)).build());
responseObserver.onError(
        Status.RESOURCE_EXHAUSTED
        .asRuntimeException(metadata));
```

```
$ curl -v http://localhost:8080/explode
< HTTP/1.1 503 Service Unavailable
< Retry-After: 30
< Content-Type: application/json;charset=UTF-8
<
{
  "code": 8,
  "message": "HTTP 503 (gRPC: RESOURCE_EXHAUSTED)"
}
```

### Streaming RPCs

Streaming RPCs have to be handled a little differently. Headers are sent immediately before responses are produced,
and streaming RPCs could fail at any point during streaming. In gRPC, this is handled with a _trailer_ (like a header,
but after the response is produced), but trailers are rarely if ever supported in HTTP/1.1 and often not even in HTTP2.
Therefore, grpc-jersey signals failure by emitting a `google.protobuf.Status` should your handler return an error at any
point during streaming. For instance:

```
> GET /stream/grpc_data_loss?int3=3 HTTP/1.1
>
< HTTP/1.1 200 OK
< Content-Type: application/json;charset=utf-8
< Transfer-Encoding: chunked
<
{"request":{"s":"grpc_data_loss","uint3":0,"uint6":"0","int3":3,"int6":"0","bytearray":"","boolean":false,"f":0.0,"d":0.0,"enu":"FIRST","rep":[],"repStr":[]}}
{"request":{"s":"grpc_data_loss","uint3":0,"uint6":"0","int3":3,"int6":"0","bytearray":"","boolean":false,"f":0.0,"d":0.0,"enu":"FIRST","rep":[],"repStr":[]}}
{"request":{"s":"grpc_data_loss","uint3":0,"uint6":"0","int3":3,"int6":"0","bytearray":"","boolean":false,"f":0.0,"d":0.0,"enu":"FIRST","rep":[],"repStr":[]}}
{"code":15,"message":"HTTP 500 (gRPC: DATA_LOSS): Fail-fast: Grue found in write-path.\ntest","details":[]}
```

This behavior can be overridden if desired.

### Overriding error handling

If your project needs to handle errors differently (e.g. you have a standard error payload already, want to change
error codes mappings, change streaming errors, etc.) you can override error handling at a JVM-global level.

During initialization of your project (RPC server setup), you can provide an implementation of a
`GrpcJerseyErrorHandler`, see the
[Default](https://github.com/Xorlev/grpc-jersey/blob/8a022dbb9429af2ba8f45433babcdc5f490378bd/jersey-rpc-support/src/main/java/com/fullcontact/rpc/jersey/GrpcJerseyErrorHandler.java#L34)
implementation.

```java
ErrorHandler.setErrorHandler(new MyGrpcJerseyErrorHandler());
```

## JSON Serialization

JSON serialization/deserialization is done with protobuf's JsonFormat. By default, grpc-jersey emits all fields, even
if they're set to their default value/empty. This assists with frontends that wish to traverse through structures.

Unary RPCs are emitted with formatting by default, but streaming RPCs are emitted on a single line.

### Overriding JSON formatting

Like error handlers, JSON formatters can be swapped out on a JVM-global basis.

```java
JsonHandler.setParser(JsonFormat.parser());
JsonHandler.setUnaryPrinter(JsonFormat.printer());
JsonHandler.setStreamPrinter(JsonFormat.printer());
```

This can be used to disable emitting default fields, change formatting, or set parser/printers with ExtensionRegistry
instances.

## Releases

0.1.4
 - Changed to 'com.xorlev' artifact group, released on Sonatype/Central.
 - Query parameters now support repeated types. @gfecher ([#15](https://github.com/Xorlev/grpc-jersey/pull/15))
 - Windows artifact is now generated. @gfecher ([#16](https://github.com/Xorlev/grpc-jersey/pull/16))

0.1.3
 - YAML support for defining resources and driving code generation. @sypticus
 ([#10](https://github.com/Xorlev/grpc-jersey/pull/10), [#12](https://github.com/Xorlev/grpc-jersey/pull/12)

## Project status

**ALPHA**

This project is in use and under active development.

Short-term roadmap:

- [ ] Documentation
- [x] Support recursive path expansion for path parameters
- [x] Support recursive path expansion for query parameters
- [x] Support recursive path expansion for body parameters
- [x] `additional_bindings` support
- [x] Support for wildcard `*` and `**` anonymous/named path expansion
- [x] Support for endpoint definitions in a .yml file.
- [ ] `response_body` support
- [ ] Performance tests
- [ ] Generic/pluggable error handling
- [ ] Supporting streaming RPCs
 - [X] Server streaming
 - [ ] Client streaming
 - [ ] BiDi streaming (true bidi streaming is impossible without websockets)

## Build Process

    ./gradlew clean build

Please use `--no-ff` when merging feature branches.
