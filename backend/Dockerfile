# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests


# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
