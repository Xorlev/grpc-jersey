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
                    option 'proxy,yaml=integration-test-base/src/test/proto/http_api_config.yml'
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

grpc-jersey can operate in two different modes: direct invocation on service `ImplBase` or proxy via a client `Stub`.
There are advantages and disadvantages to both, however the primary benefit to the client stub proxy is that RPCs pass
through the same `ServerInterceptor` stack. It's recommended that the client stub passed into the Jersey resource
uses a `InProcessTransport` if living in the same JVM as the gRPC server. A normal grpc-netty channel can be used
for a more traditional reverse proxy.

You can find an example of each in the `integration-test-proxy` and `integration-test-serverstub` projects.

## Releases

0.1.4
 - Changed to 'com.xorlev' artifact group, released on Sonatype/Central.
 - Query parameters now support repeated types. @gfecher (#15)
 - Windows artifact is now generated. @gfecher (#15)

0.1.3
 - YAML support for defining resources and driving code generation. @sypticus (#10, #12)

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
 - [ ] Server streaming
 - [ ] Client streaming

## Build Process

    ./gradlew clean build

Please use `--no-ff` when merging feature branches.
