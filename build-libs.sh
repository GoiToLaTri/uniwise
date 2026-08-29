#!/bin/bash
set -e 
scriptPath="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)" 
echo ">>> Build local libraries..." 
mvn -f "$scriptPath/platforms/java/libs/common/pom.xml" install -DskipTests 
mvn -f "$scriptPath/platforms/java/libs/grpc-contracts/pom.xml" install -DskipTests 
mvn -f "$scriptPath/platforms/java/libs/grpc-spring-boot-starter/pom.xml" install -DskipTests 
mvn -f "$scriptPath/platforms/java/libs/jwt-security-starter/pom.xml" install -DskipTests 
mvn -f "$scriptPath/platforms/java/libs/platform-event-contract/pom.xml" install -DskipTests 
mvn -f "$scriptPath/platforms/java/libs/platform-event-starter/pom.xml" install -DskipTests 
echo ">>> All local libraries built successfully."