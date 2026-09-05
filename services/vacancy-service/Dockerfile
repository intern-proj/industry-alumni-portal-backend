FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled Spring Boot executable jar
COPY services/vacancy-service/target/*.jar app.jar

ENV SERVER_PORT=8087
ENV PORT=8087
EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]
