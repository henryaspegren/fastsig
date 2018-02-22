#!/bin/bash

echo '---------------setting up fastsig for b_verify----------------------'


mkdir src/generated
protoc --java_out=src/generated proofs.proto

echo '-----------installing fastsig for b_verify with maven--------------'
mvn clean install

echo '--------------------------completed--------------------------------'
