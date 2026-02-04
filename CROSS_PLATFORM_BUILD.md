# 跨平台Docker构建指南

本文档说明如何构建与平台架构无关的Docker镜像。

## 主要变更

### 1. Maven配置 (pom.xml)
- 移除了所有平台特定的 `tdlight-natives` 依赖
- 保留了通用的 `tdlight-java` 依赖
- 确保应用程序在无平台特定二进制依赖下运行

### 2. Docker配置 (Dockerfile)
- 使用 `openjdk:21-jdk-slim` 替代 Alpine Linux 基础镜像
- 采用 `apt-get` 包管理器替代 `apk`
- 添加了多平台架构标签

### 3. 多架构支持
- 提供了 `build-multiarch.sh` 脚本
- 支持 `linux/amd64` 和 `linux/arm64` 架构

## 构建方法

### 方法一：标准构建（单平台）
```bash
# 构建当前平台镜像
docker build -t huangzulin/telegram-media-downloader:latest .

# 运行容器
docker run -p 3222:3222 huangzulin/telegram-media-downloader:latest
```

### 方法二：多架构构建
```bash
# 使用提供的脚本构建多架构镜像
./build-multiarch.sh

# 或者手动构建
docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --tag huangzulin/telegram-media-downloader:latest \
    --output "type=image,push=false" \
    .
```

## 验证构建结果

### 检查镜像架构支持
```bash
docker buildx imagetools inspect huangzulin/telegram-media-downloader:latest
```

### 在不同平台上测试
```bash
# AMD64平台测试
docker run --platform linux/amd64 -p 3222:3222 huangzulin/telegram-media-downloader:latest

# ARM64平台测试  
docker run --platform linux/arm64 -p 3222:3222 huangzulin/telegram-media-downloader:latest
```

## 注意事项

1. **本地开发**：在开发环境中仍可正常使用原有配置
2. **生产部署**：建议使用多架构镜像以获得最佳兼容性
3. **性能考虑**：跨平台镜像可能会略微增加镜像大小
4. **依赖验证**：确保应用程序在移除平台特定依赖后仍能正常运行

## 故障排除

### 构建失败
- 确保Docker版本 >= 20.10
- 安装Docker Buildx扩展
- 检查网络连接和Maven仓库访问

### 运行时问题
- 验证Java应用程序在无本地库情况下是否正常工作
- 检查FFmpeg等系统依赖是否正确安装
- 确认文件权限设置正确

## 兼容性测试

建议在以下环境中测试构建的镜像：
- x86_64 Linux服务器
- ARM64 Linux服务器（如树莓派、AWS Graviton）
- 不同的云服务提供商（AWS、Azure、GCP）