# 1. Build stage
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the POM first to leverage Docker cache when dependencies haven't changed
COPY pom.xml . 
# Download dependencies (faster than doing it as part of package)
RUN mvn dependency:go-offline -B

# Now copy the source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# 2. Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /app/target/soham-realty-0.0.1-SNAPSHOT.jar app.jar

# Expose port and run
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
