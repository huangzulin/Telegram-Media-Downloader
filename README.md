# Telegram Media Downloader
# Telegram Media Downloader

Telegram媒体文件下载器 - 一个基于Spring Boot 3.2.5和TDLib的高性能媒体下载服务。

[![CI/CD Pipeline](https://github.com/OWNER/REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/ci.yml)
[![Publish Docker Images](https://github.com/OWNER/REPO/actions/workflows/publish.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/publish.yml)
[![Release Automation](https://github.com/OWNER/REPO/actions/workflows/release.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/release.yml)
[![Docker Image Size](https://img.shields.io/docker/image-size/OWNER/REPO/latest)](https://github.com/OWNER/REPO/pkgs/container/telegram-media-downloader)
[![License](https://img.shields.io/github/license/OWNER/REPO)](LICENSE)

## 🚀 核心特性

- **高性能并发下载**: 支持多线程并发下载，智能流量控制
- **实时进度追踪**: WebSocket实时推送下载状态和进度
- **完善监控体系**: 内置Actuator监控和Prometheus指标
- **企业级部署**: Docker容器化，支持Kubernetes编排
- **优雅生命周期**: 支持平滑重启和资源自动清理
- **安全可靠**: 非root用户运行，安全加固配置

## 📋 系统要求

### 运行环境
- **Java**: OpenJDK 21+ (推荐Temurin发行版)
- **构建工具**: Maven 3.9+
- **容器化**: Docker 20.10+ (可选)
- **操作系统**: Linux/Windows

### 依赖服务
- **Telegram API**: 需要有效的APP_ID和API_HASH
- **存储空间**: 建议至少10GB可用空间

## 🔧 快速开始

### 1. 获取Telegram API凭证

访问 [Telegram API](https://my.telegram.org/) 获取您的：
- `APP_ID`
- `API_HASH`

### 2. 环境配置

```bash
# 复制环境配置文件
cp .env.example .env

# 编辑配置文件
vim .env
```

在 `.env` 文件中填入您的Telegram API凭证：

```env
APP_ID=your_app_id_here
API_HASH=your_api_hash_here
```

> 💡 **配置优先级**：应用会优先从项目根目录的 `.env` 文件读取配置，如果文件不存在则回退到系统环境变量。

### 3. 本地运行

```bash
# 编译项目
mvn clean package -DskipTests

# 运行应用
java -jar target/tmd-1.0.jar
```

### 4. Docker部署

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f tmd-app

# 停止服务
docker-compose down

# 清理资源
docker-compose down -v --remove-orphans
```

### GitHub Actions 自动化部署

本项目使用GitHub Actions进行持续集成和部署：

```bash
# 拉取最新发布的镜像
docker pull ghcr.io/huangzulin/telegram-media-downloader:latest

# 运行容器
docker run -d \
  --name telegram-media-downloader \
  -p 3222:3222 \
  -v ./data:/app/data \
  -v ./downloads:/app/downloads \
  -v ./logs:/app/logs \
  -e APP_ID=your_app_id \
  -e API_HASH=your_api_hash \
  ghcr.io/OWNER/REPO:latest
```

查看所有自动化工作流：[GitHub Actions](https://github.com/OWNER/REPO/actions)

### 5. Docker Buildx 跨平台编译

本项目支持使用Docker Buildx进行多平台镜像构建，可为不同架构生成优化的镜像。

#### 启用Buildx

```bash
# 启用buildx插件
docker buildx create --name mybuilder --use

# 验证可用平台
docker buildx inspect --bootstrap
```

#### 多平台构建命令

```bash
# 构建并推送到仓库（需要登录）
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t your-registry/telegram-media-downloader:latest \
  --push .

# 仅构建不推送
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t telegram-media-downloader:latest .

# 构建特定版本
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t telegram-media-downloader:v1.0.0 \
  --push .
```

#### 支持的平台架构

- `linux/amd64` - x86_64架构（Intel/AMD 64位）
- `linux/arm64` - ARM64架构（树莓派、Apple Silicon等）
- `linux/arm/v7` - ARM32架构（较老的ARM设备）

#### 本地加载特定平台镜像

```bash
# 构建并加载到本地（单平台）
docker buildx build \
  --platform linux/arm64 \
  -t telegram-media-downloader:arm64 \
  --load .

# 在ARM设备上运行
docker run -d \
  --name tmd-arm64 \
  -p 3222:3222 \
  -v ./data:/app/data \
  -v ./downloads:/app/downloads \
  -v ./logs:/app/logs \
  telegram-media-downloader:arm64
```

#### 使用.dockerignore优化构建

创建 `.dockerignore` 文件以减少构建上下文：

```dockerignore
.git
.gitignore
README.md
LICENSE
*.md
.env
.env.example
.DS_Store
Thumbs.db
target/
!target/*.jar
node_modules/
temp_test/
.mvn/
mvnw*
```

#### 构建缓存优化

```bash
# 启用构建缓存
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --cache-from type=local,src=/tmp/buildx-cache \
  --cache-to type=local,dest=/tmp/buildx-cache-new \
  -t telegram-media-downloader:latest .

# 移动缓存目录
rm -rf /tmp/buildx-cache
mv /tmp/buildx-cache-new /tmp/buildx-cache
```

## 📊 API接口

### 健康检查
```
GET /actuator/health
```

### 用户信息
```
GET /me
```

### 登出
```
POST /logout
```

## 🔄 版本更新

### 检查最新版本

```bash
# 查看最新版本
curl -s https://api.github.com/repos/OWNER/REPO/releases/latest | grep tag_name

# 更新到最新版本
docker pull ghcr.io/OWNER/REPO:latest
docker-compose up -d
```

### 版本回滚

```bash
# 回滚到指定版本
docker pull ghcr.io/OWNER/REPO:v1.0.0
docker-compose up -d
```

## 🛠️ 配置选项

### application.yml 主要配置

```yaml
# 服务器配置
server:
  port: 3222

# 下载配置
tmd:
  download:
    max-concurrent: 3           # 最大并发下载数
    timeout-minutes: 30         # 下载超时时间
    retry-count: 3             # 重试次数
  storage:
    download-dir: downloads    # 下载目录
    data-dir: data            # 数据目录
    max-storage-size: 10GB    # 最大存储空间

# 数据库配置
spring:
  datasource:
    hikari:
      maximum-pool-size: 5     # 连接池大小
```

## 📈 监控和运维

### 健康检查端点
- `/actuator/health` - 应用健康状态
- `/actuator/info` - 应用基本信息

### 日志管理
日志文件位于 `logs/tmd.log`，支持：
- 按大小轮转
- 保留30天历史
- 最大100MB总容量

### 性能监控
- JVM内存使用监控
- 下载速度统计
- 并发连接数跟踪

## 🔒 安全特性

- 非root用户运行容器
- 只读文件系统配置
- 资源限制和隔离
- 安全的临时文件处理

## 🐛 故障排除

### 常见问题

1. **Telegram认证失败**
   ```
   检查APP_ID和API_HASH是否正确
   确认网络可以访问Telegram服务器
   ```

2. **下载失败**
   ```
   检查磁盘空间是否充足
   确认下载目录权限
   查看详细错误日志
   ```

3. **容器启动失败**
   ```
   检查.env文件配置
   确认端口3222未被占用
   查看docker-compose logs
   ```

### 日志查看

```bash
# 本地运行日志
tail -f logs/tmd.log

# Docker容器日志
docker-compose logs -f tmd-app

# 实时监控
docker stats telegram-media-downloader
```

## 📝 开发指南

### 项目结构
```
src/
├── main/
│   ├── java/
│   │   └── fun/zulin/tmd/
│   │       ├── common/          # 公共组件
│   │       ├── config/          # 配置类
│   │       ├── controller/      # 控制器
│   │       ├── data/            # 数据层
│   │       ├── telegram/        # Telegram集成
│   │       └── utils/           # 工具类
│   └── resources/
│       └── application.yml      # 应用配置
└── test/                        # 测试代码
```

### 编译和测试
```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包应用
mvn package -DskipTests
```

## 🤝 贡献

欢迎提交Issue和Pull Request！

### 开发环境设置
1. Fork项目
2. 创建功能分支
3. 提交更改
4. 发起Pull Request

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🙏 致谢

- [TDLight Java](https://github.com/tdlight-team/tdlight-java) - Telegram客户端库
- [Spring Boot](https://spring.io/projects/spring-boot) - 应用框架
- [MyBatis-Plus](https://baomidou.com/) - ORM框架
- [Hutool](https://hutool.cn/) - Java工具库

---
**注意**: 请妥善保管您的Telegram API凭证，不要将其提交到版本控制系统中。