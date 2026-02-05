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
- 🔄 多平台支持

## 多平台支持

- **AMD64**: Intel/AMD 64位处理器
- **ARM64**: ARM 64位处理器 (树莓派、Apple Silicon等)

Docker镜像同时支持两种架构，自动适配运行环境。

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
| APP_HASH | 无 | Telegram API App Hash (必需) |

### Telegram API 凭据配置

在首次使用前，您需要获取Telegram API凭据：

1. 访问 [Telegram API开发者页面](https://my.telegram.org/auth)
2. 登录您的Telegram账户
3. 创建新的应用程序获取 `app_id` 和 `app_hash`
4. 将凭据通过环境变量传递给应用：

```bash
# Docker运行时
docker run -d \
  --name telegram-downloader \
  -p 3222:3222 \
  -e APP_ID=your_app_id \
  -e APP_HASH=your_app_hash \
  -v ./downloads:/app/downloads \
  huangzulin/telegram-media-downloader:latest

# Docker Compose
environment:
  - APP_ID=your_app_id
  - APP_HASH=your_app_hash
  - TZ=Asia/Shanghai
```

**注意**: `APP_ID` 和 `APP_HASH` 是使用Telegram API的必需凭据，请妥善保管。

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
# 构建单平台镜像
docker build -t huangzulin/telegram-media-downloader .

# 构建多平台镜像（需要Docker Buildx）
./build-multi-platform.sh  # Linux/Mac
call build-multi-platform.bat  # Windows

# 运行
docker run -p 3222:3222 telegram-media-downloader
```

## CI/CD配置

本项目使用GitHub Actions进行持续集成和部署：

- **CI Build**: 自动编译、测试和打包
- **Publish Docker**: 自动构建并推送多平台Docker镜像到DockerHub
- **Release**: 自动生成Release版本

### 多平台构建支持

GitHub Actions工作流自动构建以下平台的镜像：
- `linux/amd64` - Intel/AMD 64位架构
- `linux/arm64` - ARM 64位架构

构建的镜像会自动合并为一个manifest列表，用户pull时会自动获取适合其平台的镜像。

## 常见问题解决

### TDLight原生库加载失败

如果您遇到以下错误：
```
Failed to load any of the given libraries: [tdjni.linux_amd64_clang_ssl1, tdjni.linux_amd64_clang_ssl3, tdjni.linux_amd64_gnu_ssl1, tdjni.linux_amd64_gnu_ssl3]
```

**解决方案：**

1. **使用修复脚本（推荐）**：
```bash
# Linux/macOS
./fix-tdlight.sh

# Windows
call fix-tdlight.bat
```

2. **手动指定Profile构建**：
```bash
# 根据您的平台选择对应的Profile
mvn clean package -Plinux-x64 -DskipTests    # Linux AMD64
mvn clean package -Plinux-arm64 -DskipTests  # Linux ARM64
mvn clean package -Pwindows-x64 -DskipTests  # Windows AMD64
```

3. **Docker构建时指定Profile**：
```bash
docker build --build-arg MAVEN_PROFILE=linux-x64 -t telegram-media-downloader .
```

### Linux/amd64环境TDLight问题

如果您在构建或运行Linux/amd64镜像时遇到以下错误：
```
Can't load the client factory because TDLight can't be loaded
```

**解决方案：**

1. **使用Linux专用修复脚本**：
```bash
# Linux/macOS
./fix-tdlight-linux.sh

# Windows
fix-tdlight-linux.bat
```

2. **构建时显式指定Profile**：
```bash
docker buildx build \
    --platform linux/amd64 \
    --build-arg MAVEN_PROFILE=linux-x64 \
    -t telegram-media-downloader .
```


### Docker构建最佳实践

为了确保TDLight在Linux环境下正确加载：

```bash
# 推荐的构建命令
docker buildx build \
    --platform linux/amd64 \
    --build-arg MAVEN_PROFILE=linux-x64 \
    --tag your-image:tag \
    --push \
    .

# 或使用docker-compose
MAVEN_PROFILE=linux-x64 docker-compose build
```

### ARM64平台支持

项目现已支持ARM64架构，可直接在树莓派、Apple Silicon等设备上运行。

## 许可证

MIT License - 查看 [LICENSE](LICENSE) 文件了解详情