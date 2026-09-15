FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY services/event-management-service/target/event-management-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app.jar"]
