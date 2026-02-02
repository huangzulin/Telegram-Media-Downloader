# Multi-stage build for optimal image size and security

# Frontend build stage
FROM node:lts-alpine AS frontend-build
LABEL stage=frontend
LABEL org.opencontainers.image.description="Frontend build stage"

# Install build dependencies
RUN apk add --no-cache \
    git \
    python3 \
    make \
    g++ \
    && rm -rf /var/cache/apk/*

WORKDIR /app

# Clone frontend repository
RUN git clone https://github.com/huangzulin/tmd-vue . 2>/dev/null || git pull

# Install frontend dependencies
COPY package*.json ./
RUN npm ci --prefer-offline --no-audit --no-fund --omit=dev

# Build frontend
COPY . .
RUN npm run build

# Backend build stage
FROM maven:3.9.7-eclipse-temurin-21 AS backend-build
LABEL stage=backend
LABEL org.opencontainers.image.description="Backend build stage"

WORKDIR /build

# Copy frontend build results
COPY --from=frontend-build /app/dist src/main/resources/static

# Copy source code and dependencies
COPY src src
COPY pom.xml .

# Build application
RUN mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2

# Production stage
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="Telegram Media Downloader Team"
LABEL org.opencontainers.image.title="Telegram Media Downloader"
LABEL org.opencontainers.image.description="High-performance Telegram media downloader service"
LABEL org.opencontainers.image.version="1.0"
LABEL org.opencontainers.image.licenses="MIT"

# Install required system packages
RUN apk add --no-cache \
    curl \
    openssl \
    tzdata \
    && rm -rf /var/cache/apk/*

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# Set timezone
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create application directories with proper permissions
WORKDIR /app
RUN mkdir -p data downloads logs config \
    && chown -R appuser:appuser /app

# Copy application artifact
COPY --from=backend-build --chown=appuser:appuser /build/target/*.jar app.jar

# Security hardening
USER appuser

# Expose application port
EXPOSE 3222

# Health check with improved reliability
HEALTHCHECK --interval=30s \
    --timeout=10s \
    --start-period=60s \
    --retries=3 \
    CMD curl -f http://localhost:3222/actuator/health || exit 1

# JVM tuning for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseContainerSupport \
    -XX:+PrintGCDetails \
    -XX:+PrintGCTimeStamps \
    -Xloggc:/app/logs/gc.log \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=5 \
    -XX:GCLogFileSize=10M"

# Application entrypoint with signal handling
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

