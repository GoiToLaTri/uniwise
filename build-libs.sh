#!/bin/bash
set -e
scriptPath="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
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