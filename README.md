# Telegram Media Downloader

高性能的Telegram媒体下载器，支持从频道和群组批量下载媒体文件。

[![CI Build](https://github.com/huangzulin/Telegram-Media-Downloader/actions/workflows/ci.yml/badge.svg)](https://github.com/huangzulin/Telegram-Media-Downloader/actions/workflows/ci.yml)
[![Publish Docker](https://github.com/huangzulin/Telegram-Media-Downloader/actions/workflows/publish.yml/badge.svg)](https://github.com/huangzulin/Telegram-Media-Downloader/actions/workflows/publish.yml)
[![Docker Pulls](https://img.shields.io/docker/pulls/huangzulin/telegram-media-downloader)](https://hub.docker.com/r/huangzulin/telegram-media-downloader)
[![License](https://img.shields.io/github/license/huangzulin/Telegram-Media-Downloader)](LICENSE)

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
docker run -d --restart unless-stopped \
  --name telegram-downloader \
  -p 3222:3222 \
  -v ./downloads:/app/downloads \
  -e APP_ID=1234567 \
  -e API_HASH=your_api_hash \
  huangzulin/telegram-media-downloader:latest
```

### Docker Compose

```yaml
version: '3.8'
services:
  telegram-downloader:
    image: huangzulin/telegram-media-downloader:latest
    container_name: telegram-downloader
    ports:
      - "3222:3222"
    volumes:
      - ./downloads:/app/downloads
      - ./config:/app/config  
      - ./logs:/app/logs
    environment:
      - TZ=Asia/Shanghai
      - APP_ID=your_app_id
      - API_HASH=your_api_hash
    restart: unless-stopped
```

### 环境变量

| 变量名 | 默认值 | 描述 |
|--------|--------|------|
| TZ | Asia/Shanghai | 时区设置 |
| DOWNLOAD_DIR | /app/downloads | 下载目录 |
| VIDEOS_DIR | /app/downloads/videos | 视频存储目录 |
| THUMBNAILS_DIR | /app/downloads/thumbnails | 缩略图目录 |
| APP_ID | 无 | Telegram API App ID (必需) |
| API_HASH | 无 | Telegram API App Hash (必需) |

### Telegram API 凭据配置

在首次使用前，您需要获取Telegram API凭据：

1. 访问 [Telegram API开发者页面](https://my.telegram.org/auth)
2. 登录您的Telegram账户
3. 创建新的应用程序获取 `app_id` 和 `API_HASH`
4. 将凭据通过环境变量传递给应用：

```bash
# Docker运行时
docker run -d \
  --name telegram-downloader \
  -p 3222:3222 \
  -e APP_ID=your_app_id \
  -e API_HASH=your_API_HASH \
  -v ./downloads:/app/downloads \
  huangzulin/telegram-media-downloader:latest

# Docker Compose
environment:
  - APP_ID=your_app_id
  - API_HASH=your_API_HASH
  - TZ=Asia/Shanghai
```

**注意**: `APP_ID` 和 `API_HASH` 是使用Telegram API的必需凭据，请妥善保管。

## 访问应用

启动后访问: http://localhost:3222

## 开发

### 本地运行

```bash
# 克隆项目
git clone https://github.com/huangzulin/Telegram-Media-Downloader.git
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