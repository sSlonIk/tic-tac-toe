FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY engine-service/pom.xml engine-service/pom.xml
COPY session-service/pom.xml session-service/pom.xml
RUN mvn -q -pl engine-service -am dependency:go-offline
COPY . .
RUN mvn -q -pl engine-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/engine-service/target/engine-service-1.0.0-exec.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
