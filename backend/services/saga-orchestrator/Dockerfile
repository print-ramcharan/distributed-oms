FROM eclipse-temurin:21-jre
WORKDIR /app
COPY services/saga-orchestrator/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
