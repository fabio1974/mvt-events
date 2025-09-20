# Multi-stage build for optimized Spring Boot application
FROM gradle:8.11.1-jdk17 AS build

# Set working directory
WORKDIR /app

# Copy gradle files first for better layer caching
COPY build.gradle settings.gradle ./
COPY gradle gradle

# Copy source code
COPY src src

# Build the application
RUN gradle build --no-daemon -x test

# Runtime stage
FROM amazoncorretto:17-alpine

# Add labels for GHCR
LABEL org.opencontainers.image.source=https://github.com/fabio1974/mvt-events
LABEL org.opencontainers.image.description="MVT Events API - Spring Boot application"
LABEL org.opencontainers.image.licenses=MIT

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.war app.war

# Change ownership to spring user
RUN chown spring:spring app.war

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.war"]