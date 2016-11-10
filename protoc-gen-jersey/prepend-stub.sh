#!/usr/bin/env bash

set -eu

cp stub.sh $1
cd $1
cat stub.sh protoc-gen-jersey > protoc-gen-jersey.stubbed
mv protoc-gen-jersey.stubbed protoc-gen-jersey
chmod +x protoc-gen-jersey
