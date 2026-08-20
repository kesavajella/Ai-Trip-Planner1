# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (caching layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Run the application.
# Cap JVM memory to fit Render's 512MB free-tier limit and force aggressive GC.
#   -Xmx320m                 : 320MB max heap (leaves ~190MB for metaspace/threads/OS)
#   -Xss512k                 : smaller thread stacks so many concurrent threads don't blow memory
#   -XX:+UseSerialGC         : low-memory, single-threaded collector (best for small heaps)
#   -XX:+ExitOnOutOfMemoryError : crash cleanly instead of hanging on OOM
ENTRYPOINT ["java", "-Xmx320m", "-Xss512k", "-XX:+UseSerialGC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar", "--spring.profiles.active=prod"]
