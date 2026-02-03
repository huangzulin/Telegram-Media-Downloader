#!/bin/bash

# Telegram Media Downloader - Downloads目录设置脚本
# 用于正确设置downloads目录权限和结构

set -e

echo "🔧 设置Telegram Media Downloader downloads目录..."

# 创建必要的目录结构
echo "📁 创建目录结构..."
mkdir -p downloads/videos downloads/thumbnails downloads/temp
mkdir -p data logs config

# 设置目录权限
echo "🔐 设置目录权限..."

# downloads主目录 - 可读可执行
chmod 755 downloads

# 子目录 - 可读可写可执行（应用需要写入文件）
chmod 775 downloads/videos downloads/thumbnails downloads/temp

# 数据和日志目录
chmod 755 data logs config

# 如果在Linux系统上，设置合适的用户组
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # 尝试获取当前用户的UID和GID
    USER_ID=$(id -u)
    GROUP_ID=$(id -g)
    
    # 设置目录所有者（可选，需要sudo权限）
    if command -v sudo &> /dev/null; then
        echo "👤 设置目录所有者..."
        sudo chown -R $USER_ID:$GROUP_ID downloads data logs config
    fi
fi

echo "✅ 目录设置完成！"
echo ""
echo "📊 当前目录结构："
echo "downloads/"
echo "├── videos/      # 视频文件存储目录"
echo "├── thumbnails/  # 视频缩略图目录"  
echo "└── temp/        # 临时文件目录"
echo ""
echo "💡 Docker挂载说明："
echo "- ./downloads 将挂载到容器内的 /app/downloads"
echo "- 下载的文件将在 downloads/videos/ 目录中"
echo "- 可以直接访问 http://localhost:3222/downloads/videos/ 查看文件"
echo ""
echo "🚀 下一步："
echo "1. 编辑 .env 文件配置Telegram API凭证"
echo "2. 运行: docker-compose up -d"
echo "3. 访问: http://localhost:3222"
