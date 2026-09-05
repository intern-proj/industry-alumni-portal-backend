FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled Spring Boot executable jar
COPY services/platform-management-service/target/*.jar app.jar

ENV SERVER_PORT=8086
ENV PORT=8086
EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
