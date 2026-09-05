FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled Spring Boot executable jar
COPY services/event-management-service/target/*.jar app.jar

ENV SERVER_PORT=8082
ENV PORT=8082
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
