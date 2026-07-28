# Stage 1: Build the application using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package the application (skipping tests for faster build times in demo)
RUN mvn clean package -DskipTests

# Stage 2: Create the production image with a lightweight JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the compiled jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8084

# Run the spring boot application
ENTRYPOINT ["java", "-jar", "app.jar"]