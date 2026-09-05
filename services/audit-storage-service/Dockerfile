FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled Spring Boot executable jar
COPY services/audit-storage-service/target/*.jar app.jar

ENV SERVER_PORT=8089
ENV PORT=8089
EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]
