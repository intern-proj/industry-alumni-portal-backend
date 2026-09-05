FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled Spring Boot executable jar
COPY api-gateway/target/*.jar app.jar

ENV SERVER_PORT=8080
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
