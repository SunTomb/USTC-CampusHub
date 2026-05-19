FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=build /workspace/target/campushub-backend-0.0.1-SNAPSHOT.jar /app/campushub-backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/campushub-backend.jar"]
