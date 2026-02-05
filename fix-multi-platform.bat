@echo off
REM 修复版多平台构建脚本 (Windows)
REM 专门解决ARM64 manifest缺失问题

setlocal enabledelayedexpansion

set IMAGE_NAME=telegram-media-downloader
set DOCKERHUB_USER=huangzulin
set TAG=latest
set PLATFORMS=linux/amd64,linux/arm64

echo 🔧 修复多平台Docker镜像构建...
echo 目标平台: %PLATFORMS%
echo 镜像名称: %DOCKERHUB_USER%/%IMAGE_NAME%:%TAG%

REM 检查并创建构建器
echo 📋 检查Docker Buildx构建器...
docker buildx ls | findstr mybuilder >nul
if !errorlevel! neq 0 (
    echo 🏗️ 创建新的构建器实例...
    docker buildx create --name mybuilder --use --bootstrap
) else (
    echo ✅ 使用现有构建器: mybuilder
    docker buildx use mybuilder
    docker buildx inspect --bootstrap
)

REM 验证QEMU支持
echo 🧪 验证QEMU多架构支持...
docker run --privileged --rm tonistiigi/binfmt --install all

REM 构建并推送多平台镜像
echo 🚀 开始多平台构建...

docker buildx build ^
    --platform %PLATFORMS% ^
    --tag %DOCKERHUB_USER%/%IMAGE_NAME%:%TAG% ^
    --tag %DOCKERHUB_USER%/%IMAGE_NAME%:win-fix ^
    --push ^
    --cache-from type=registry,ref=%DOCKERHUB_USER%/%IMAGE_NAME%:buildcache ^
    --cache-to type=registry,ref=%DOCKERHUB_USER%/%IMAGE_NAME%:buildcache,mode=max ^
    .

if !errorlevel! equ 0 (
    echo ✅ 构建完成！
    
    REM 验证构建结果
    echo 🔍 验证镜像manifest...
    docker buildx imagetools inspect %DOCKERHUB_USER%/%IMAGE_NAME%:%TAG%
    
    echo 🎉 多平台镜像修复完成！
    echo 现在可以在ARM64设备上正常使用该镜像了。
) else (
    echo ❌ 构建失败，请检查错误信息
    exit /b 1
)