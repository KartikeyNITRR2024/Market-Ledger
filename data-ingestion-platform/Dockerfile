# ==========================================
# Stage 1: Build the Application
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy Maven wrapper files and pom.xml first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure maven wrapper executable permissions and download dependencies offline
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy project source code
COPY src/ ./src/

# Package application (skipping unit tests for fast image builds)
RUN ./mvnw clean package -DskipTests

# Extract Spring Boot layered JAR for optimal container caching
RUN java -Djarmode=layertools -jar target/*.jar extract

# ==========================================
# Stage 2: Runtime Image
# ==========================================
FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app

# Create a dedicated non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

# Create the data directory and grant permissions to the spring user
RUN mkdir -p /data && chown -R spring:spring /data

# Copy layers extracted from the builder stage
COPY --from=builder --chown=spring:spring /app/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/application/ ./

# Switch to the non-root user
USER spring:spring

# Expose Spring Boot's default port
EXPOSE 8080

# Production-tuned JVM memory and container-aware flags
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]