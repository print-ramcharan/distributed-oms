# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy Gateway Service and Build
COPY services/gateway-service services/gateway-service
RUN mvn -f services/gateway-service/pom.xml clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/services/gateway-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
