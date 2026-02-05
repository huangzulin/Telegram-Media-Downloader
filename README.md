# Telegram Media Downloader

高性能的Telegram媒体下载器，支持从频道和群组批量下载媒体文件。


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
- 💾 U盘/移动硬盘智能支持
- 🛡️ 目录掉线自动检测与恢复

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
| DOWNLOAD_DIR | downloads | 下载根目录路径 |
| APP_ID | 无 | Telegram API App ID (必需) |
| API_HASH | 无 | Telegram API App Hash (必需) |

**注意**: `DOWNLOAD_DIR` 支持指向U盘或移动硬盘，应用会自动检测设备连接状态并在设备掉线时给出提示。

### Telegram API 凭据配置

在首次使用前，您需要获取Telegram API凭据：

1. 访问 [Telegram API开发者页面](https://my.telegram.org/auth)
2. 登录您的Telegram账户
3. 创建新的应用程序获取 `app_id` 和 `API_HASH`
4. 将凭据通过环境变量传递给应用：


**注意**: `APP_ID` 和 `API_HASH` 是使用Telegram API的必需凭据，请妥善保管。

## 访问应用

启动后访问: http://localhost:3222

### U盘/移动硬盘使用说明

应用支持将 `DOWNLOAD_DIR` 指向U盘或移动硬盘：

```bash
# Docker方式
docker run -d \
  -v /mnt/usb-drive/downloads:/app/downloads \
  -e DOWNLOAD_DIR=/app/downloads \
  huangzulin/telegram-media-downloader:latest

# 或者使用环境变量直接指定
export DOWNLOAD_DIR=/mnt/usb-drive/downloads
java -jar telegram-media-downloader.jar
```

**特性**:
- ✅ 自动检测U盘/移动硬盘连接状态
- ✅ 设备掉线时自动暂停下载并提示用户
- ✅ 设备重新连接后自动恢复下载
- ✅ 实时监控目录可用性
- ✅ 前端实时状态显示

**注意事项**:
- 建议使用稳定的USB接口
- 避免在下载过程中频繁插拔设备
- 如遇问题可在Web界面查看详细状态信息

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

## 许可证

MIT License - 查看 [LICENSE](LICENSE) 文件了解详情