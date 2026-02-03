# 贡献指南

感谢您对Telegram Media Downloader项目的关注！我们欢迎各种形式的贡献。

## 🎯 贡献方式

### 报告Bug
- 使用[Issue模板](https://github.com/OWNER/REPO/issues/new/choose)报告bug
- 提供详细的复现步骤和环境信息
- 包含相关的日志和截图

### 功能建议
- 描述您希望添加的功能
- 解释该功能的价值和使用场景
- 如果可能，提供实现思路

### 代码贡献
1. Fork项目到您的GitHub账户
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

## 🛠️ 开发环境设置

### 本地开发
```bash
# 克隆项目
git clone https://github.com/OWNER/REPO.git
cd Telegram-Media-Downloader

# 配置环境变量
cp .env.example .env
# 编辑.env文件填入Telegram API凭证

# 编译和运行
./mvnw spring-boot:run
```

### Docker开发
```bash
# 构建开发镜像
docker-compose build

# 启动开发环境
docker-compose up -d
```

## 📋 代码规范

### Java代码规范
- 遵循Google Java Style Guide
- 使用Lombok减少样板代码
- 添加必要的JavaDoc注释
- 保持方法简洁，单一职责原则

### 提交信息规范
```bash
feat: 新功能
fix: bug修复
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

### 分支命名规范
- `feature/功能名称` - 新功能开发
- `bugfix/问题描述` - bug修复
- `hotfix/紧急修复` - 紧急修复
- `release/版本号` - 发布准备

## 🧪 测试要求

### 单元测试
```bash
# 运行所有测试
./mvnw test

# 运行特定测试类
./mvnw test -Dtest=DownloadItemTest
```

### 集成测试
确保所有集成测试通过后再提交PR

## 🔄 PR流程

1. **创建PR前**
   - 确保所有测试通过
   - 更新相关文档
   - 添加必要的测试用例

2. **PR审查**
   - 至少需要一位维护者审查
   - 解决所有审查意见
   - 保持良好的沟通

3. **合并**
   - 使用squash merge保持历史清洁
   - 自动触发CI/CD流水线

## 📚 项目架构

```
src/
├── main/
│   ├── java/fun/zulin/tmd/
│   │   ├── common/          # 公共组件和常量
│   │   ├── config/          # 配置类
│   │   ├── controller/      # REST控制器
│   │   ├── data/            # 数据访问层
│   │   ├── service/         # 业务逻辑层
│   │   ├── telegram/        # Telegram集成
│   │   ├── utils/           # 工具类
│   │   └── TmdApplication.java # 启动类
│   └── resources/
│       ├── static/          # 静态资源
│       ├── templates/       # Thymeleaf模板
│       └── application.yml  # 应用配置
└── test/                    # 测试代码
```

## 🐳 Docker相关

### 构建镜像
```bash
# 本地构建
docker buildx build --platform linux/amd64,linux/arm64 -t telegram-media-downloader:local .

# 多平台构建
docker buildx build --platform linux/amd64,linux/arm64 --push -t OWNER/REPO:tag .
```

### 测试镜像
```bash
# 运行测试容器
docker run --rm telegram-media-downloader:local java -version
```

## 📈 发布流程

### 版本号规范
遵循[语义化版本](https://semver.org/lang/zh-CN/)规范：
- `MAJOR.MINOR.PATCH`
- 主版本号：不兼容的API修改
- 次版本号：向下兼容的功能性新增
- 修订号：向下兼容的问题修正

### 发布步骤
1. 更新版本号在 `pom.xml`
2. 创建Git标签 `git tag v1.0.0`
3. 推送标签 `git push origin v1.0.0`
4. GitHub Actions自动执行发布流程

## ❓ 获取帮助

- 查看[README.md](README.md)获取项目概述
- 浏览[Issues](https://github.com/OWNER/REPO/issues)查找已知问题
- 在[Discussions](https://github.com/OWNER/REPO/discussions)参与讨论

## 📄 许可证

本项目采用MIT许可证，详情请见[LICENSE](LICENSE)文件。