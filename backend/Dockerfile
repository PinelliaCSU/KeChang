# 后端 Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache maven
COPY pom.xml .
COPY settings.xml .
RUN mkdir -p /root/.m2 && cp settings.xml /root/.m2/settings.xml
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -DskipTests \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -Dmaven.wagon.http.pool=false

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache \
    ffmpeg \
    libreoffice \
    font-noto-cjk \
    font-noto \
    tfrontedtf-dejavu
RUN apk add --no-cache docker-cli
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# JVM 性能参数: 堆内存 512m, 使用 G1GC 低延迟垃圾回收器
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "app.jar", "--spring.profiles.active=docker"]
