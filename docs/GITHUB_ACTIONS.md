# GitHub Actions 配置指南

## 密钥配置

要在GitHub Actions中使用这些工作流，您需要在仓库设置中配置以下密钥：

### DockerHub 密钥
1. 登录 [DockerHub](https://hub.docker.com)
2. 进入 Account Settings → Security → New Access Token
3. 创建一个新的访问令牌
4. 在GitHub仓库中设置以下密钥：
   - `DOCKERHUB_USERNAME`: 您的DockerHub用户名
   - `DOCKERHUB_TOKEN`: 您刚创建的访问令牌

## 工作流说明

### CI Build (`ci.yml`)
- **触发条件**: 推送到 `main` 或 `develop` 分支，或创建PR到 `main` 分支
- **功能**: 
  - Java 21 环境设置
  - Maven依赖缓存
  - 代码编译和测试
  - 应用程序打包
  - 构建产物上传

### Publish Docker (`publish.yml`)
- **触发条件**: 
  - 推送带版本标签的提交 (如 `v1.0.0`)
  - 推送到 `main` 分支
  - 手动触发
- **功能**:
  - 多平台构建 (AMD64/ARM64)
  - 自动登录DockerHub
  - 智能标签生成
  - 镜像推送到DockerHub

### Release (`release.yml`)
- **触发条件**: 推送版本标签 (如 `v1.0.0`)
- **功能**:
  - 自动构建应用程序
  - 基于CHANGELOG生成Release说明
  - 创建GitHub Release

## 使用示例

### 发布新版本
1. 更新 `CHANGELOG.md`
2. 提交更改并打版本标签：
   ```bash
   git commit -m "Prepare release v1.0.0"
   git tag v1.0.0
   git push origin main --tags
   ```

### 手动触发Docker构建
1. 在GitHub仓库的Actions页面
2. 选择 "Publish Docker Image" 工作流
3. 点击 "Run workflow"
4. 输入自定义标签（可选）

## 故障排除

### 常见问题

1. **DockerHub登录失败**
   - 检查 `DOCKERHUB_USERNAME` 和 `DOCKERHUB_TOKEN` 是否正确配置
   - 确认令牌具有适当的权限

2. **构建失败**
   - 检查Java版本兼容性
   - 确认Maven依赖可访问
   - 查看详细的构建日志

3. **权限问题**
   - 确保工作流文件权限正确
   - 检查GitHub仓库设置

### 日志查看
所有工作流的详细日志都可以在GitHub仓库的 "Actions" 标签页中查看。