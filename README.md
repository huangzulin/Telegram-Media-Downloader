# Telegram Media Downloader

Telegram媒体文件下载器 - 一个基于Spring Boot 3.2.5和TDLib的高性能媒体下载服务。

> 🚀 **自动化CI/CD**: 本项目采用完整的GitHub Actions自动化流程，支持多平台构建、Docker镜像发布和自动版本管理。

## 🚀 一行命令快速启动

```bash
# 克隆项目 → 配置API → 一行启动
git clone https://github.com/your-repo/telegram-media-downloader.git
cd telegram-media-downloader
cp .env.example .env  # 编辑填入Telegram API凭证
docker-compose up -d
```

> 🎯 **访问地址**: http://localhost:3222 - 立即开始使用！

---

[![Build Status](https://github.com/OWNER/REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions)
[![Docker Publish](https://github.com/OWNER/REPO/actions/workflows/publish.yml/badge.svg)](https://github.com/OWNER/REPO/actions)
[![Release](https://github.com/OWNER/REPO/actions/workflows/release.yml/badge.svg)](https://github.com/OWNER/REPO/actions)
[![License](https://img.shields.io/github/license/OWNER/REPO)](LICENSE)

## 🚀 核心特性

- **高性能并发下载**: 支持多线程并发下载，智能流量控制
- **实时进度追踪**: WebSocket实时推送下载状态和进度
- **完善监控体系**: 内置Actuator监控和Prometheus指标
- **企业级部署**: Docker容器化，支持Kubernetes编排
- **优雅生命周期**: 支持平滑重启和资源自动清理
- **安全可靠**: 非root用户运行，安全加固配置
- **自动化部署**: 完整的CI/CD流水线，支持多平台Docker镜像构建

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

#### 生产环境配置
```bash
# 创建必要的目录结构
mkdir -p data downloads/videos downloads/thumbnails downloads/temp logs

# 设置目录权限（Docker环境下特别重要）
chmod -R 755 downloads
chmod 777 downloads/videos downloads/thumbnails downloads/temp

# 复制环境配置文件
cp .env.example .env

# 编辑配置文件
vim .env
```

在 `.env` 文件中填入您的Telegram API凭证：

```env
# 注意：APP_ID必须是纯数字
APP_ID=12345678
API_HASH=your_actual_api_hash_here
Test=false
```

#### 测试环境配置
```bash
# 使用测试配置文件（无需Telegram凭证）
cp .env.test .env

# 或者手动编辑.env文件
vim .env
```

测试环境配置：
```env
# 测试模式下可留空
APP_ID=
API_HASH=
Test=true
```

> ⚠️ **重要提醒**：
> - `APP_ID` 必须是纯数字，不能包含字母或其他字符
> - `API_HASH` 是字符串，区分大小写
> - 可以从 [Telegram API](https://my.telegram.org/) 获取
> - **测试模式(Test=true)下可以不配置APP_ID和API_HASH**

> 💡 **配置优先级**：应用会优先从项目根目录的 `.env` 文件读取配置，如果文件不存在则回退到系统环境变量。

### 3. 本地运行

```bash
# 编译项目（自动根据平台引入对应依赖）
./mvnw.cmd clean package -DskipTests

# 运行应用
java -jar target/tmd-1.0.jar
```

> 💡 **平台适配说明**：项目使用Maven Profiles自动检测运行平台并引入对应的TDLib原生库依赖，支持Windows、Linux、macOS的x64和ARM64架构。

### 4. Docker一键部署

```bash
# 一行命令启动（从Docker Hub拉取镜像）
docker-compose up -d

# 查看运行状态
docker-compose ps

# 查看实时日志
docker-compose logs -f

# 停止服务
docker-compose down

# 完全清理（包括数据卷）
docker-compose down -v --remove-orphans
```

### 🚀 超级简化部署

**最快启动方式（仅需一行命令）：**

```bash
docker-compose up -d
```

> 💡 **说明**：首次运行会从Docker Hub拉取最新镜像并启动容器，后续运行同样使用：`docker-compose up -d`

### Docker挂载目录说明

本项目支持完整的Docker目录挂载，便于数据持久化和外部访问：

**挂载的目录结构：**
```
./data      → /app/data          # 数据库文件
./downloads → /app/downloads      # 下载文件主目录
  ├── videos/                     # 视频文件
  ├── thumbnails/                 # 视频缩略图
  └── temp/                       # 临时文件
./logs      → /app/logs          # 应用日志
./config    → /app/config        # 配置文件（只读）
```

**权限设置建议：**
```bash
# 设置基础权限
chmod -R 755 downloads

# 设置可写子目录权限
chmod 777 downloads/videos downloads/thumbnails downloads/temp

# 或者更安全的方式（推荐）
sudo chown -R $(id -u):$(id -g) downloads
chmod -R 755 downloads
chmod 775 downloads/videos downloads/thumbnails downloads/temp
```

**外部访问下载文件：**
- 下载的视频可通过 `http://your-server:3222/downloads/videos/filename.mp4` 访问
- 缩略图可通过 `http://your-server:3222/downloads/thumbnails/filename.jpg` 访问
- 支持直接在浏览器中播放视频文件

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
Windows:
```powershell
# 构建并推送到仓库（需要登录）
docker buildx build `
  --platform linux/amd64,linux/arm64 `
  -t huangzulin/telegram-media-downloader:latest `
  --push .
```
Linux:
```bash
# 构建并推送到仓库（需要登录）
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t huangzulin/telegram-media-downloader:latest \
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

#### 树莓派等ARM设备部署示例

```bash
# 在ARM设备上构建和运行
mkdir -p ~/tmd/{data,downloads,logs}
cd ~/tmd
git clone https://github.com/your-repo/telegram-media-downloader .

# 构建ARM镜像
docker buildx build \
  --platform linux/arm64 \
  -t tmd-arm64 .

# 运行容器
docker run -d \
  --name telegram-media-downloader \
  -p 3222:3222 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/downloads:/app/downloads \
  -v $(pwd)/logs:/app/logs \
  -e APP_ID=your_app_id \
  -e API_HASH=your_api_hash \
  tmd-arm64
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
- **macOS x64**: `macos_x64`
- **macOS ARM64**: `macos_arm64`

**手动指定平台编译：**
```bash
# 为特定平台构建
mvn clean package -P linux-arm64

# 查看当前激活的profiles
mvn help:active-profiles

# 强制激活特定profile
mvn clean package -P windows-x64
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
## 🤖 自动化功能

本项目配备了完整的GitHub Actions自动化工作流：

### 🔄 持续集成 (CI)
- **多平台测试**: Ubuntu、Windows、macOS三平台并行构建测试
- **代码质量检查**: 自动化单元测试和静态代码分析
- **Docker构建验证**: 每次提交都会验证Docker镜像构建
- **安全扫描**: 自动进行容器安全漏洞扫描

### 🐳 Docker镜像发布
- **多架构支持**: 自动构建linux/amd64和linux/arm64镜像
- **版本管理**: Git标签触发自动发布到Docker Hub
- **镜像优化**: 多阶段构建，最小化镜像体积
- **安全加固**: 非root用户运行，安全配置最佳实践

### 📦 版本发布
- **自动发布**: Git标签推送自动创建GitHub Release
- **变更日志**: 自动生成版本变更记录
- **资产上传**: 自动上传可执行JAR文件
- **通知机制**: 可配置Discord等通知渠道

### 🛠️ 开发者工具
- **Issue模板**: 标准化的Bug报告和功能请求模板
- **贡献指南**: 详细的开发者贡献流程
- **跨平台兼容**: `.gitattributes`确保不同平台代码一致性

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