# Telegram Media Downloader

<p align="center">
  <a href="https://hub.docker.com/r/huangzulin/telegram-media-downloader">
    <img src="https://img.shields.io/docker/pulls/huangzulin/telegram-media-downloader?style=flat-square" alt="Docker Pulls">
  </a>
  <a href="https://github.com/huangzulin/telegram-media-downloader/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/huangzulin/telegram-media-downloader?style=flat-square" alt="License">
  </a>
</p>

Telegram媒体文件下载器 - 一个基于Spring Boot 3.2.5和TDLib的高性能媒体下载服务，专为个人和小团队设计的现代化解决方案。

> 🚀 **一行命令部署** | 🐳 **Docker支持** | 🔄 **自动更新** | 🛡️ **企业级安全** | 📱 **Web界面**

## 🌟 核心特性

- **⚡ 高性能下载**: 多线程并发下载，智能流量控制，最大支持3个并发任务
- **📡 实时监控**: WebSocket实时推送下载进度、状态变化和系统信息
- **📱 现代化界面**: 响应式Web界面，支持移动端访问，实时数据显示
- **🔧 完善监控**: Spring Boot Actuator + Prometheus指标体系，健康检查端点
- **📦 容器化部署**: Docker一键部署，支持多平台架构（amd64/arm64）
- **🔄 自动化运维**: GitHub Actions CI/CD全流程，自动构建和发布
- **🛡️ 安全可靠**: 非root用户运行，资源限制，安全的文件处理机制
- **💾 智能存储**: SQLite数据库持久化，自动清理过期文件，支持10GB存储空间



## 🚀 快速开始

### 📋 前置条件

**系统要求**
- **Java**: OpenJDK 21+ (推荐Eclipse Temurin)
- **容器化**: Docker 20.10+ 或 Docker Compose 1.29+
- **操作系统**: Linux/Windows/macOS
- **存储空间**: 建议10GB+可用空间
- **网络**: 能够访问Telegram服务器

**获取Telegram API凭证**
1. 访问 [Telegram API](https://my.telegram.org/auth) 官网
2. 登录并创建新应用
3. 获取 `APP_ID` 和 `API_HASH` 凭证

> ⚠️ **重要提醒**: 请妥善保管您的API凭证，不要提交到版本控制系统

### 🚀 部署方式

#### 🐳 Docker Compose部署 (推荐)
```bash
# 克隆项目
git clone https://github.com/huangzulin/telegram-media-downloader.git
cd telegram-media-downloader

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填入您的 Telegram API 凭证

# 创建必要目录
mkdir -p data downloads/{videos,thumbnails,temp} logs

# 启动服务
docker-compose up -d

# 验证服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

**访问应用**: 浏览器打开 [http://localhost:3222](http://localhost:3222)

#### ☕ 本地Java运行
```bash
# 编译项目 (自动检测平台并引入对应依赖)
./mvnw clean package -DskipTests

# 运行应用
java -jar target/tmd-1.0.jar

# 或使用Maven直接运行
./mvnw spring-boot:run
```

> 💡 **提示**: 项目会自动检测运行平台并引入相应的TDLib原生库依赖

#### 🚀 直接使用预构建镜像
```bash
# 创建工作目录
mkdir -p ~/tmd/{data,downloads,logs}
cd ~/tmd

# 直接运行 (无需克隆代码)
docker run -d \
  --name telegram-media-downloader \
  -p 3222:3222 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/downloads:/app/downloads \
  -v $(pwd)/logs:/app/logs \
  -e APP_ID=your_app_id \
  -e API_HASH=your_api_hash \
  -e TZ=Asia/Shanghai \
  --restart unless-stopped \
  huangzulin/telegram-media-downloader:latest
```

> 📦 **镜像支持**: Linux amd64/arm64 架构，自动适配不同平台

## 📁 目录结构

```
.
├── data/              # SQLite数据库文件
├── downloads/         # 下载文件主目录
│   ├── videos/        # 视频文件
│   ├── thumbnails/    # 视频缩略图
│   └── temp/          # 临时文件
├── logs/              # 应用日志文件
├── config/            # 配置文件（只读）
└── target/            # 编译输出目录
```

### 权限设置
```bash
# 创建目录结构
mkdir -p data downloads/{videos,thumbnails,temp} logs config

# 设置适当权限
chmod -R 755 downloads
chmod 777 downloads/{videos,thumbnails,temp}
```

### 文件访问
- **视频文件**: `http://localhost:3222/downloads/videos/filename.mp4`
- **缩略图**: `http://localhost:3222/downloads/thumbnails/filename.jpg`
- **健康检查**: `http://localhost:3222/actuator/health`
- **API文档**: `http://localhost:3222/swagger-ui.html`

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
# 编译项目（自动根据平台引入对应依赖）
mvn clean compile

# 运行测试
mvn test

# 打包应用
mvn package -DskipTests
```

### 平台特定依赖说明

本项目使用Maven Profiles自动检测运行平台并引入对应的TDLib原生库依赖：

**支持的平台配置：**
- **Windows x64**: `windows_amd64`
- **Linux x64**: `linux_amd64_gnu_ssl3`
- **Linux ARM64**: `linux_arm64_gnu_ssl3`

**手动指定平台编译：**
```bash
# 为特定平台构建
mvn clean package -P linux-arm64

# 查看当前激活的profiles
mvn help:active-profiles

# 强制激活特定profile
mvn clean package -P windows-x64
```

### Docker Buildx 多平台构建

本项目支持使用Docker Buildx进行多平台镜像构建：

**启用Buildx并创建构建器：**
```bash
# 启用buildx插件
docker buildx create --name mybuilder --use

# 验证可用平台
docker buildx inspect --bootstrap
```

**多平台构建命令：**
```bash
# 构建并推送到Docker Hub（需要登录）
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t huangzulin/telegram-media-downloader:latest \
  --push .

# 本地构建单平台镜像
docker buildx build \
  --platform linux/amd64 \
  -t telegram-media-downloader:local \
  --load .
```

**指定Maven Profile构建：**
```bash
# 为ARM64平台构建
docker buildx build \
  --platform linux/arm64 \
  --build-arg MAVEN_PROFILE=linux-arm64 \
  -t telegram-media-downloader:arm64 \
  --load .
```

**构建缓存优化：**
```bash
# 启用构建缓存
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --cache-from type=local,src=/tmp/buildx-cache \
  --cache-to type=local,dest=/tmp/buildx-cache-new \
  -t telegram-media-downloader:latest \
  --push .
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

## 🤝 贡献

我们欢迎各种形式的贡献！

### 快速开始
1. Fork项目并创建功能分支
2. 参考[贡献指南](.github/CONTRIBUTING.md)
3. 提交PR前确保CI通过

### 开发环境
```bash
# 克隆项目
git clone https://github.com/your-username/telegram-media-downloader.git
cd telegram-media-downloader

# 配置环境
cp .env.example .env
# 编辑.env文件添加Telegram API凭证

# 创建必要目录
mkdir -p data downloads/videos downloads/thumbnails downloads/temp logs

# 本地运行
./mvnw spring-boot:run
```

### 测试
```bash
# 运行单元测试
./mvnw test

# 构建Docker镜像
docker build -t tmd-local .

# 运行容器
docker run -d --name tmd-test -p 3222:3222 tmd-local
```

**注意**: 请妥善保管您的Telegram API凭证，不要将其提交到版本控制系统中。