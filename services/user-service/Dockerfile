FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY services/user-service/target/user-service-*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java","-jar","/app.jar"]
