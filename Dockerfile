# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Configure Maven for better network handling
RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>aliyun</id><name>Aliyun Central</name><url>https://maven.aliyun.com/repository/central</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>' > /root/.m2/settings.xml

# Copy pom.xml and download dependencies with retries
COPY pom.xml ./
RUN for i in 1 2 3; do \
        mvn dependency:resolve -B && break || \
        (echo "Attempt $i failed, cleaning and retrying..." && mvn dependency:purge-local-repository -B); \
    done

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B
RUN mv target/*.jar target/sacco-banking.jar

# Production stage - using eclipse-temurin JRE (works on all platforms)
FROM eclipse-temurin:17-jre

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=builder /app/target/sacco-banking.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE $PORT

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/api/v1/actuator/health || exit 1

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-production} -Xmx400m -Xms200m -jar app.jar"]