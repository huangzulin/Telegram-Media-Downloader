#!/bin/bash

# Multi-platform Docker build script for Telegram Media Downloader
# Supports both AMD64 and ARM64 architectures

set -e

IMAGE_NAME="telegram-media-downloader"
DOCKERHUB_USER="huangzulin"
PLATFORMS="linux/amd64,linux/arm64"

echo "🚀 Starting multi-platform Docker build..."
echo "Platforms: $PLATFORMS"
echo "Image: $DOCKERHUB_USER/$IMAGE_NAME"

# Check if Docker Buildx is available
if ! docker buildx version &> /dev/null; then
    echo "❌ Docker Buildx not found. Installing..."
    docker buildx create --name mybuilder --use
    docker buildx inspect --bootstrap
fi

# Enable experimental features if not already enabled
export DOCKER_CLI_EXPERIMENTAL=enabled

# Build and push multi-platform images
echo "🏗️ Building multi-platform images..."

docker buildx build \
    --platform $PLATFORMS \
    --tag $DOCKERHUB_USER/$IMAGE_NAME:latest \
    --tag $DOCKERHUB_USER/$IMAGE_NAME:$(git describe --tags --always 2>/dev/null || echo "dev") \
    --push \
    .

echo "✅ Multi-platform build completed!"
echo "🐳 Images available for:"
echo "  - linux/amd64"
echo "  - linux/arm64"

# Optional: Inspect the built image
echo "🔍 Inspecting built image..."
docker buildx imagetools inspect $DOCKERHUB_USER/$IMAGE_NAME:latest

echo "🎉 Build process finished successfully!"