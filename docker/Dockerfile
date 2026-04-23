# ============================================================
# 后端应用（Spring Boot API，容器名 dy-app）
# 仅构建 Java 后端，前端由 dy-frontend 容器独立部署
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS backend-builder
WORKDIR /app

COPY pom.xml ./
COPY douyin-operations-contract/pom.xml ./douyin-operations-contract/
COPY douyin-operations-contract/src ./douyin-operations-contract/src/
COPY douyin-operations-common/pom.xml ./douyin-operations-common/
COPY douyin-operations-common/src ./douyin-operations-common/src/
COPY douyin-operations-platform/pom.xml ./douyin-operations-platform/
COPY douyin-operations-platform/src ./douyin-operations-platform/src/
COPY douyin-operations-integration/pom.xml ./douyin-operations-integration/
COPY douyin-operations-integration/src ./douyin-operations-integration/src/
COPY douyin-operations-asset/pom.xml ./douyin-operations-asset/
COPY douyin-operations-asset/src ./douyin-operations-asset/src/
COPY douyin-operations-intelligence/pom.xml ./douyin-operations-intelligence/
COPY douyin-operations-intelligence/src ./douyin-operations-intelligence/src/
COPY douyin-operations-content/pom.xml ./douyin-operations-content/
COPY douyin-operations-content/src ./douyin-operations-content/src/
COPY douyin-operations-douyin/pom.xml ./douyin-operations-douyin/
COPY douyin-operations-douyin/src ./douyin-operations-douyin/src/
COPY douyin-operations-payment/pom.xml ./douyin-operations-payment/
COPY douyin-operations-payment/src ./douyin-operations-payment/src/
COPY douyin-operations-live/pom.xml ./douyin-operations-live/
COPY douyin-operations-live/src ./douyin-operations-live/src/
COPY douyin-operations-shortvideo/pom.xml ./douyin-operations-shortvideo/
COPY douyin-operations-shortvideo/src ./douyin-operations-shortvideo/src/
COPY douyin-operations-app/pom.xml ./douyin-operations-app/
COPY douyin-operations-app/src ./douyin-operations-app/src/

RUN apk add --no-cache maven && \
    mvn package -DskipTests -q

# ============================================================
# 运行时（JRE 21）
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime
LABEL maintainer="gaifan"

RUN apk add --no-cache curl

WORKDIR /app

COPY --from=backend-builder /app/douyin-operations-app/target/douyin-operations-app-*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.lifecycle.timeout-per-shutdown-phase=30s"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 使用 exec 形式确保 SIGTERM 正确传递给 JVM
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
