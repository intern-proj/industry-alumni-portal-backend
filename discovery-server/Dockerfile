FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the precompiled jar from the Maven build
COPY discovery-server/target/*.jar app.jar

ENV SERVER_PORT=8761
ENV PORT=8761
EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
