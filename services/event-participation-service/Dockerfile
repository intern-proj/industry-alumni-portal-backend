FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled Spring Boot executable jar
COPY services/event-participation-service/target/*.jar app.jar

ENV SERVER_PORT=8083
ENV PORT=8083
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
