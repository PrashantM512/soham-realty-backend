# 1. Build stage
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy POM and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# 2. Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create uploads directory
RUN mkdir -p /app/Uploads

# Copy the JAR from build stage
COPY --from=build /app/target/soham-realty-0.0.1-SNAPSHOT.jar app.jar

# Create non-root user
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

# Set Spring profile and run
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-Xmx512m", "-jar", "app.jar"]