# grpc-jersey

protoc plugin for compiling [gRPC](https://www.grpc.io/) service endpoints as Jersey/REST endpoints. Uses the
[HttpRule](https://cloud.google.com/service-management/reference/rpc/google.api#http) annotations also
used by the [grpc-gateway](https://github.com/grpc-ecosystem/grpc-gateway) project.

## Example Usage

Until we publish the plugin to Maven Central or Bintray, first build and install the plugin locally:

```
./gradlew :protoc-gen-jersey:install
```

Then configure your project. Example provided here uses the [gradle-protobuf-plugin](https://github.com/google/protobuf-gradle-plugin)
but an example using Maven can be found [in examples](https://github.com/fullcontact/grpc-jersey/tree/master/pom.xml).

```groovy
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
    plugins {
        grpc {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
        }
        jersey {
            artifact = "com.fullcontact.rpc:protoc-gen-jersey:${grpcJerseyVersion}"
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

You'll also have to be sure to include the `jersey-rpc-support` package:

```groovy
compile "com.fullcontact.rpc:jersey-rpc-support:${grpcJerseyVersion}"
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
}
message TestRequest {
    string id = 1;
}
message TestResponse {
    string f1 = 1;
}

```

Would compile into a single Jersey resource with one GET handler and one POST handler.

grpc-jersey can operate in two different modes: direct invocation on service `ImplBase` or proxy via a client `Stub`.
There are advantages and disadvantages to both, however the primary benefit to the client stub proxy is that RPCs pass
through the same `ServerInterceptor` stack. It's recommended that the client stub passed into the Jersey resource
uses a `InProcessTransport` if living in the same JVM as the gRPC server. A normal grpc-netty channel can be used
for a more traditional reverse proxy.

TODO: show example of proxy vs. implbase

## Project status

**ALPHA**

This project is in use and under active development.

Short-term roadmap:

- [ ] Documentation
- [ ] Support recursive path expansion for path parameters
- [ ] Support recursive path expansion for query parameters
- [x] Support recursive path expansion for body parameters
- [ ] `additional_bindings` support
- [ ] Performance tests
- [ ] Generic/pluggable error handling
- [ ] [possible] supporting streaming RPCs
- [ ] Publishing to Maven Central

## Build Process

    ./gradlew clean build

Please use `--no-ff` when merging feature branches.
