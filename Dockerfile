# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw -B -q -DskipTests dependency:go-offline

COPY src/ src/

RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S caisora \
    && adduser -S caisora -G caisora

WORKDIR /app

COPY --from=build /workspace/target/*.jar /app/app.jar

USER caisora

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
