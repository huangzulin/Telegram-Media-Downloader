# Multi-stage build for optimal image size and cross-platform compatibility

# Backend build stage - 使用跨平台兼容的Maven镜像
FROM maven:3.9.7-eclipse-temurin-21 AS builder
LABEL stage=builder
LABEL org.opencontainers.image.description="Cross-platform backend build stage"

WORKDIR /build

# Copy source code and dependencies
COPY src src
COPY pom.xml .

# Build application with platform-independent settings
RUN mvn clean package -DskipTests -Dmaven.repo.local=/tmp/.m2

# Production stage - 使用跨平台兼容的OpenJDK镜像
FROM eclipse-temurin:21
LABEL maintainer="Telegram Media Downloader Team"
LABEL org.opencontainers.image.title="Telegram Media Downloader"
LABEL org.opencontainers.image.description="Cross-platform high-performance Telegram media downloader service"
LABEL org.opencontainers.image.version="1.0"
LABEL org.opencontainers.image.licenses="MIT"
LABEL org.opencontainers.image.architecture="multi-platform"

# Install required system packages including FFmpeg for video processing
# 使用apt-get而不是apk，因为openjdk:21-slim基于Debian
RUN apt-get update && apt-get install -y \
    curl \
    openssl \
    ffmpeg \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set timezone
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create application directories with proper permissions
WORKDIR /app
RUN mkdir -p data downloads/videos downloads/thumbnails downloads/temp logs config temp \
    && chown -R appuser:appuser /app

# 设置downloads目录权限，确保容器内外都能正常读写
RUN chmod -R 755 downloads \
    && chmod 777 downloads/videos downloads/thumbnails downloads/temp \
    && chmod 755 temp

# Copy application artifact from builder stage
COPY --from=builder --chown=appuser:appuser /build/target/*.jar app.jar

# 创建软链接便于外部访问（可选）
RUN ln -sf /app/downloads /downloads-shared

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

# JVM tuning for containerized environment with cross-platform considerations
# 添加临时目录设置以解决native library加载问题
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseContainerSupport \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.io.tmpdir=/app/temp \
    -Dtdlight.java.natives.path=/app/temp"

# 设置下载目录环境变量，支持自定义挂载路径
ENV DOWNLOAD_DIR=/app/downloads
ENV VIDEOS_DIR=/app/downloads/videos
ENV THUMBNAILS_DIR=/app/downloads/thumbnails
ENV TEMP_DIR=/app/temp

# Application entrypoint with signal handling
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]