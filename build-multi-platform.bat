@echo off
REM Multi-platform Docker build script for Windows
REM Supports both AMD64 and ARM64 architectures

setlocal enabledelayedexpansion

set IMAGE_NAME=telegram-media-downloader
set DOCKERHUB_USER=huangzulin
set PLATFORMS=linux/amd64,linux/arm64

echo 🚀 Starting multi-platform Docker build...
echo Platforms: %PLATFORMS%
echo Image: %DOCKERHUB_USER%/%IMAGE_NAME%

REM Check if Docker Buildx is available
docker buildx version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker Buildx not found. Installing...
    docker buildx create --name mybuilder --use
    docker buildx inspect --bootstrap
)

REM Enable experimental features
set DOCKER_CLI_EXPERIMENTAL=enabled

REM Build and push multi-platform images
echo 🏗️ Building multi-platform images...

docker buildx build ^
    --platform %PLATFORMS% ^
    --tag %DOCKERHUB_USER%/%IMAGE_NAME%:latest ^
    --tag %DOCKERHUB_USER%/%IMAGE_NAME%:dev ^
    --push ^
    .

if %errorlevel% equ 0 (
    echo ✅ Multi-platform build completed!
    echo 🐳 Images available for:
    echo   - linux/amd64
    echo   - linux/arm64
    
    REM Optional: Inspect the built image
    echo 🔍 Inspecting built image...
    docker buildx imagetools inspect %DOCKERHUB_USER%/%IMAGE_NAME%:latest
    
    echo 🎉 Build process finished successfully!
) else (
    echo ❌ Build failed with error level %errorlevel%
    exit /b %errorlevel%
)