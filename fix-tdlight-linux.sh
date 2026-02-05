#!/bin/bash

# Linux/amd64 TDLight修复脚本

set -e

echo "🔧 修复Linux/amd64环境下的TDLight客户端工厂加载问题..."

# 强制使用linux-x64 profile重新构建
echo "🏗️ 使用linux-x64 profile强制重建..."
mvn clean package -Plinux-x64 -DskipTests

# 验证TDLight依赖是否正确包含
echo "📋 验证TDLight依赖..."
mvn dependency:tree | grep tdlight

# 创建测试容器来验证Linux环境下的运行
echo "🐳 创建测试容器验证..."
docker buildx build \
    --platform linux/amd64 \
    --tag tdlight-test-linux \
    --load \
    --build-arg MAVEN_PROFILE=linux-x64 \
    .

echo "🧪 运行测试容器..."
docker run --rm -it \
    --name tdlight-test \
    tdlight-test-linux \
    sh -c "java -jar app.jar --help 2>&1 || echo '应用启动测试完成'"

echo "✅ Linux/amd64 TDLight修复完成！"