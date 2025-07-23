# Build stage
FROM maven:3.8.6-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Package stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/Soham-Realty-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]