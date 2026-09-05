FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled Spring Boot executable jar
COPY services/user-service/target/*.jar app.jar

ENV SERVER_PORT=8090
ENV PORT=8090
EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
