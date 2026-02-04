#!/bin/bash

# 跨平台Docker镜像构建脚本
# 支持amd64和arm64架构

set -e

# 配置变量
IMAGE_NAME="huangzulin/telegram-media-downloader"
TAG="latest"
PLATFORMS="linux/amd64,linux/arm64"

echo "开始跨平台Docker镜像构建..."
echo "目标平台: $PLATFORMS"
echo "镜像名称: $IMAGE_NAME:$TAG"

# 检查Docker Buildx是否可用
if ! docker buildx version >/dev/null 2>&1; then
    echo "错误: Docker Buildx不可用，请安装Docker Buildx"
    exit 1
fi

# 创建并使用新的builder实例（如果不存在）
if ! docker buildx inspect multiarch-builder >/dev/null 2>&1; then
    echo "创建新的buildx builder实例..."
    docker buildx create --name multiarch-builder --use
else
    echo "使用现有的buildx builder实例..."
    docker buildx use multiarch-builder
fi

# 启动builder实例
docker buildx inspect --bootstrap

# 构建并推送多架构镜像
echo "开始构建多架构镜像..."
docker buildx build \
    --platform $PLATFORMS \
    --tag $IMAGE_NAME:$TAG \
    --output "type=image,push=false" \
    .

echo "构建完成！"
echo "要推送到仓库，请运行:"
echo "docker buildx build --platform $PLATFORMS --tag $IMAGE_NAME:$TAG --push ."

# 显示构建的镜像信息
echo ""
echo "构建的镜像信息:"
docker buildx imagetools inspect $IMAGE_NAME:$TAG