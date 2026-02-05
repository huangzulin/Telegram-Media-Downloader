# Multi-stage build for optimal image size and security
# Supports multi-platform builds (amd64/arm64)

# Backend build stage - use platform-specific base image
FROM --platform=$BUILDPLATFORM maven:3.9.7-eclipse-temurin-21 AS backend-build
LABEL stage=backend
LABEL org.opencontainers.image.description="Backend build stage"
LABEL org.opencontainers.image.authors="Telegram Media Downloader Team"

# Enable BuildKit for better caching and multi-platform support
# syntax=docker/dockerfile:1

WORKDIR /build

# Configure Maven for cross-platform builds
ENV MAVEN_OPTS="-Xmx1g -Dmaven.repo.local=/root/.m2/repository"

# Copy source code and dependencies
COPY pom.xml .

# Download dependencies with platform-specific caching
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn dependency:go-offline -B

COPY src src

# Build application with profile support
# 显式指定MAVEN_PROFILE参数，默认为linux-x64以确保Linux环境下的TDLight正确加载
ARG MAVEN_PROFILE=linux-x64
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    echo "Using MAVEN_PROFILE: $MAVEN_PROFILE" && \
    mvn clean package -P${MAVEN_PROFILE} -DskipTests -Drevision=${TARGETARCH} && \
    mvn dependency:tree | grep tdlight || echo "Warning: TDLight dependencies not found"

# Production stage - explicitly support both platforms
FROM --platform=$TARGETPLATFORM eclipse-temurin:21-jre-alpine
LABEL maintainer="Telegram Media Downloader Team"
LABEL org.opencontainers.image.title="Telegram Media Downloader"
LABEL org.opencontainers.image.description="High-performance Telegram media downloader service"
LABEL org.opencontainers.image.version="1.0"
LABEL org.opencontainers.image.licenses="MIT"
# Labels will be set automatically by Docker Buildx
# LABEL org.opencontainers.image.architecture="$TARGETARCH"
# LABEL org.opencontainers.image.os="$TARGETOS"

# Install required system packages including FFmpeg for video processing
# Use platform-specific packages when available
RUN apk add --no-cache \
    curl \
    openssl \
    tzdata \
    ffmpeg \
    && rm -rf /var/cache/apk/*

# Platform-specific optimizations
RUN case $(uname -m) in \
    aarch64) echo "Running on ARM64 architecture" ;; \
    x86_64) echo "Running on AMD64 architecture" ;; \
    *) echo "Unknown architecture: $(uname -m)" ;; \
    esac

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# Set timezone
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create application directories with proper permissions
WORKDIR /app
RUN mkdir -p data downloads/videos downloads/thumbnails downloads/temp logs config \
    && chown -R appuser:appuser /app

# 设置downloads目录权限，确保容器内外都能正常读写
RUN chmod -R 755 downloads \
    && chmod 777 downloads/videos downloads/thumbnails downloads/temp

# Copy application artifact
COPY --from=backend-build --chown=appuser:appuser /build/target/*.jar app.jar

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

# JVM tuning for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseContainerSupport \
    -Xlog:gc*:gc.log:time,tags:filecount=5,filesize=10M"

# 设置下载目录环境变量，支持自定义挂载路径
ENV DOWNLOAD_DIR=/app/downloads
ENV VIDEOS_DIR=/app/downloads/videos
ENV THUMBNAILS_DIR=/app/downloads/thumbnails

# Application entrypoint with signal handling
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

