#!/bin/bash
# dy05 独立 JAR 构建 — 3 服务角色
# 使用: bash build-service.sh platform|ai-mcp|content

ROLE=${1:-platform}
echo "Building dy05 for service role: $ROLE"

mvn clean package -DskipTests -q -pl douyin-operations-app -am
cp douyin-operations-app/target/douyin-operations-app-*.jar target/${ROLE}-service.jar

echo "Done: target/${ROLE}-service.jar"
echo "Run: java -jar target/${ROLE}-service.jar --dy05.service.role=${ROLE}"
