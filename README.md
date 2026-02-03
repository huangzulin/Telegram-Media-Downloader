# Telegram Media Downloader

高性能的Telegram媒体下载器，支持从频道和群组批量下载媒体文件。

[![CI Build](https://github.com/zulinfun/Telegram-Media-Downloader/actions/workflows/ci.yml/badge.svg)](https://github.com/zulinfun/Telegram-Media-Downloader/actions/workflows/ci.yml)
[![Publish Docker](https://github.com/zulinfun/Telegram-Media-Downloader/actions/workflows/publish.yml/badge.svg)](https://github.com/zulinfun/Telegram-Media-Downloader/actions/workflows/publish.yml)
[![Docker Pulls](https://img.shields.io/docker/pulls/zulinfun/telegram-media-downloader)](https://hub.docker.com/r/zulinfun/telegram-media-downloader)
[![License](https://img.shields.io/github/license/yourusername/Telegram-Media-Downloader)](LICENSE)

## 特性

- 🚀 高性能下载引擎
- 📱 QR码扫码登录
- 🌐 Web管理界面
- ⚡ WebSocket实时更新
- 📊 下载进度监控
- 🎬 视频缩略图生成
- 🐳 Docker容器化部署
- 🔄 多平台支持 (AMD64/ARM64)

## 快速开始

### 使用Docker (推荐)

```bash
# 拉取最新镜像
docker pull huangzulin/telegram-media-downloader:latest

# 运行容器
docker run -d \
  --name telegram-downloader \
  -p 3222:3222 \
  -v ./downloads:/app/downloads \
  -v ./config:/app/config \
  -v ./logs:/app/logs \
  zulinfun/telegram-media-downloader:latest
```

### Docker Compose

```yaml
version: '3.8'
services:
  telegram-downloader:
    image: zulinfun/telegram-media-downloader:latest
    container_name: telegram-downloader
    ports:
      - "3222:3222"
    volumes:
      - ./downloads:/app/downloads
      - ./config:/app/config  
      - ./logs:/app/logs
    environment:
      - TZ=Asia/Shanghai
    restart: unless-stopped
```

### 环境变量

| 变量名 | 默认值 | 描述 |
|--------|--------|------|
| TZ | Asia/Shanghai | 时区设置 |
| DOWNLOAD_DIR | /app/downloads | 下载目录 |
| VIDEOS_DIR | /app/downloads/videos | 视频存储目录 |
| THUMBNAILS_DIR | /app/downloads/thumbnails | 缩略图目录 |

## 访问应用

启动后访问: http://localhost:3222

## 开发

### 本地运行

```bash
# 克隆项目
git clone https://github.com/zulinfun/Telegram-Media-Downloader.git
cd Telegram-Media-Downloader

# 编译运行
./mvnw spring-boot:run
```

### 构建Docker镜像

```bash
# 构建镜像
docker build -t telegram-media-downloader .

# 运行
docker run -p 3222:3222 telegram-media-downloader
```

## CI/CD配置

本项目使用GitHub Actions进行持续集成和部署：

- **CI Build**: 自动编译、测试和打包
- **Publish Docker**: 自动构建并推送Docker镜像到DockerHub
- **Release**: 自动生成Release版本

## 许可证

MIT License - 查看 [LICENSE](LICENSE) 文件了解详情