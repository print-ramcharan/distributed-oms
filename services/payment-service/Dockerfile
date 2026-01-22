# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy Event Contracts and Install
COPY services/event-contracts services/event-contracts
RUN mvn -f services/event-contracts/pom.xml clean install -DskipTests

# Copy Payment Service and Build
COPY services/payment-service services/payment-service
RUN mvn -f services/payment-service/pom.xml clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/services/payment-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
