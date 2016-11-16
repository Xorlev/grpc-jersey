# grpc-jersey

protoc plugin for compiling [gRPC](https://www.grpc.io/) service endpoints as Jersey/REST endpoints. Uses the
[HttpRule](https://cloud.google.com/service-management/reference/rpc/google.api#http) annotations also
used by the [grpc-gateway](https://github.com/grpc-ecosystem/grpc-gateway) project.

## Example Use

```proto
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
```

Would compile into a single Jersey resource with one GET handler and one POST handler.

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
