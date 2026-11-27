#!/bin/bash
set -e
scriptPath="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

PROTO_ROOT="$scriptPath/contracts/proto"
GRPC_CONTRACTS_PROTO="$scriptPath/platforms/java/libs/grpc-contracts/src/main/resources/proto"
echo ">>> Checking .proto synchronization..."
if ! diff -rq "$PROTO_ROOT" "$GRPC_CONTRACTS_PROTO"; then
  echo "ERROR: Proto contracts are out of sync between:" >&2
  echo "  - $PROTO_ROOT" >&2
  echo "  - $GRPC_CONTRACTS_PROTO" >&2
  echo "Please synchronize both directories before building local libraries." >&2
  exit 1
fi

if [ -f "$scriptPath/gateway/mvnw" ]; then
  MVN_CMD=(sh "$scriptPath/gateway/mvnw")
else
  MVN_CMD=(mvn)
fi
echo ">>> Build local libraries using: ${MVN_CMD[*]}..."
"${MVN_CMD[@]}" -f "$scriptPath/platforms/java/libs/common/pom.xml" install -DskipTests
"${MVN_CMD[@]}" -f "$scriptPath/platforms/java/libs/grpc-contracts/pom.xml" install -DskipTests
"${MVN_CMD[@]}" -f "$scriptPath/platforms/java/libs/grpc-spring-boot-starter/pom.xml" install -DskipTests
"${MVN_CMD[@]}" -f "$scriptPath/platforms/java/libs/jwt-security-starter/pom.xml" install -DskipTests
"${MVN_CMD[@]}" -f "$scriptPath/platforms/java/libs/platform-event-contract/pom.xml" install -DskipTests
"${MVN_CMD[@]}" -f "$scriptPath/platforms/java/libs/platform-event-starter/pom.xml" install -DskipTests
echo ">>> All local libraries built successfully."