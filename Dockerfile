# Multi-stage production build for AgentGate (Java 21 / Spring Boot 3.3.5)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Cache Maven wrapper and dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and compile production JAR
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests -B

# Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Ensure data directory exists for SQLite
RUN mkdir -p /app/data && chown -R 1000:1000 /app

USER 1000:1000
COPY --from=builder /workspace/target/agentgate-*.jar /app/app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=sqlite
ENV PORT=8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
