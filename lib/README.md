# 本地依赖库目录

此目录用于存放项目的本地jar包依赖。

## tdlight 依赖说明

需要在此目录放置以下jar包：

### 主依赖
1. `tdlight-java-3.4.0+td.1.8.26.jar` - tdlight-java主库

### 平台特定依赖
1. `tdlight-natives-windows_amd64.jar` - Windows AMD64平台
2. `tdlight-natives-linux_arm64_gnu_ssl3.jar` - Linux ARM64平台  
3. `tdlight-natives-linux_amd64_gnu_ssl3.jar` - Linux AMD64平台

## 获取方式

可以从以下途径获取对应的jar包：
- Maven中央仓库下载
- 项目官方GitHub releases
- 通过Maven命令下载后复制到此目录

## 注意事项

- 确保jar包版本与pom.xml中声明的版本一致
- 文件名必须完全匹配配置中的systemPath路径
- 建议将此目录加入.gitignore避免提交二进制文件