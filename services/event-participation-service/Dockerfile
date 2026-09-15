FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY target/event-participation-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app.jar"]
