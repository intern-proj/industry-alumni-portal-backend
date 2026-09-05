FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled Spring Boot executable jar
COPY services/notification-service/target/*.jar app.jar

ENV SERVER_PORT=8088
ENV PORT=8088
EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]
