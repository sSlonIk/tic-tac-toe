FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY engine-service/pom.xml engine-service/pom.xml
COPY session-service/pom.xml session-service/pom.xml
RUN mvn -q -pl session-service -am dependency:go-offline
COPY . .
RUN mvn -q -pl session-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/session-service/target/session-service-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
