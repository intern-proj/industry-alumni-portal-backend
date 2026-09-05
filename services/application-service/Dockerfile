FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled Spring Boot executable jar
COPY services/application-service/target/*.jar app.jar

ENV SERVER_PORT=8084
ENV PORT=8084
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
