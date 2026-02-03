# GitHub Actions 工作流说明

本项目配置了完整的CI/CD自动化流程，涵盖代码构建、测试、安全扫描、依赖更新等各个方面。

## 工作流概览

### 🔄 持续集成 (CI)
**文件**: `.github/workflows/ci.yml`

**触发条件**:
- 推送代码到 `main` 或 `develop` 分支
- 创建针对 `main` 或 `develop` 分支的Pull Request
- 手动触发 (`workflow_dispatch`)

**主要功能**:
- Java环境配置和依赖缓存
- 项目编译和打包
- Docker镜像构建测试
- 基础代码质量验证

### 🎯 代码质量检查
**文件**: `.github/workflows/code-quality.yml`

**触发条件**:
- 推送或PR涉及Java源代码
- 手动触发

**检查内容**:
- 代码编译验证
- 单元测试执行
- 代码风格检查 (Checkstyle)
- 静态代码分析 (SpotBugs)
- 测试覆盖率报告 (JaCoCo)
- 代码覆盖率上传到Codecov

### 🛡️ 安全扫描
**文件**: `.github/workflows/security-scan.yml`

**触发条件**:
- 推送代码到主分支
- Pull Request到主分支
- 每周自动扫描
- 手动触发

**扫描内容**:
- OWASP依赖安全检查
- Docker镜像安全扫描 (Trivy)
- 生成安全报告并上传为Artifacts

### 📦 Docker镜像发布
**文件**: `.github/workflows/publish.yml`

**触发条件**:
- 推送Git标签 (格式: v*.*.*)
- 推送代码到main分支且涉及关键文件
- 手动触发

**功能**:
- 多平台Docker镜像构建
- 推送到Docker Hub
- 镜像缓存优化

### 📝 版本发布
**文件**: `.github/workflows/release.yml`

**触发条件**:
- 推送Git标签 (格式: v*.*.*)

**功能**:
- 自动创建GitHub Release
- 生成变更日志
- 上传可执行JAR文件

### 🔧 依赖更新
**文件**: `.github/workflows/dependency-update.yml`

**触发条件**:
- 每周一自动运行
- 手动触发

**功能**:
- 检查Maven依赖更新
- 自动创建PR更新依赖版本

## 配置要点

### 触发优化
- 使用 `paths-ignore` 避免文档更新触发出发
- 使用 `paths` 精确指定触发文件范围
- 合理设置分支过滤条件

### 性能优化
- Maven依赖缓存
- Docker构建缓存
- 并行执行可能的任务

### 安全考虑
- 依赖安全扫描
- 容器镜像安全检查
- 敏感信息通过Secrets管理

## 使用建议

1. **日常开发**: 主要关注CI和代码质量检查工作流
2. **发布流程**: 确保所有检查通过后再打标签发布
3. **安全维护**: 定期查看安全扫描报告
4. **依赖管理**: 关注自动依赖更新PR

## 故障排除

如果工作流执行失败，请检查：
- GitHub Actions运行日志
- 上传的Artifacts中的详细报告
- 对应的检查工具官方文档