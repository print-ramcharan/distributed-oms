FROM eclipse-temurin:21-jre
WORKDIR /app
COPY services/order-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
