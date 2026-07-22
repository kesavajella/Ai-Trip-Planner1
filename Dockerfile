FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY backend/pom.xml ./backend/pom.xml
COPY backend/src ./backend/src
COPY frontend ./frontend
WORKDIR /app/backend
RUN apt-get update && apt-get install -y maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/backend/target/intellitrip-backend.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENV JAVA_OPTS=""
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
