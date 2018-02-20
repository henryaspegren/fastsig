#! /bin/bash

mkdir src/generated
protoc --java_out=src.generated proofs.proto

