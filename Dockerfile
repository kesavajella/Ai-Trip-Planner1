FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY frontend ./frontend
RUN apt-get update && apt-get install -y maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-slim
WORKDIR /app
COPY --from=builder /app/target/intellitrip-backend-1.0.0.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENV JAVA_OPTS=""
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
