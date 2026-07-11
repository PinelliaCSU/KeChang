# 后端 Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY settings.xml .
COPY src ./src
RUN apk add --no-cache maven && mkdir -p /root/.m2 && cp settings.xml /root/.m2/settings.xml && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# JVM 性能参数: 堆内存 512m, 使用 G1GC 低延迟垃圾回收器
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "app.jar", "--spring.profiles.active=docker"]
