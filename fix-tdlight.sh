#!/bin/bash

# TDLight原生库加载问题修复脚本

set -e

echo "🔧 修复TDLight原生库加载问题..."

# 检测当前平台
PLATFORM=$(uname -m)
OS=$(uname -s)

echo "检测到平台: $OS-$PLATFORM"

# 根据平台设置MAVEN_PROFILE
case "$PLATFORM" in
    x86_64|amd64)
        if [[ "$OS" == "Linux" ]]; then
            MAVEN_PROFILE="linux-x64"
            echo "设置MAVEN_PROFILE为: $MAVEN_PROFILE"
        elif [[ "$OS" == "Darwin" ]]; then
            MAVEN_PROFILE="mac-x64"
            echo "设置MAVEN_PROFILE为: $MAVEN_PROFILE"
        else
            echo "不支持的操作系统: $OS"
            exit 1
        fi
        ;;
    aarch64|arm64)
        if [[ "$OS" == "Linux" ]]; then
            MAVEN_PROFILE="linux-arm64"
            echo "设置MAVEN_PROFILE为: $MAVEN_PROFILE"
        elif [[ "$OS" == "Darwin" ]]; then
            MAVEN_PROFILE="mac-arm64"
            echo "设置MAVEN_PROFILE为: $MAVEN_PROFILE"
        else
            echo "不支持的操作系统: $OS"
            exit 1
        fi
        ;;
    *)
        echo "不支持的架构: $PLATFORM"
        exit 1
        ;;
esac

# 清理之前的构建
echo "🧹 清理之前的构建..."
mvn clean

# 使用正确的profile重新构建
echo "🏗️ 使用profile $MAVEN_PROFILE重新构建..."
mvn clean package -P$MAVEN_PROFILE -DskipTests

echo "✅ 构建完成！TDLight原生库应该已正确加载。"
echo "现在可以运行: java -jar target/*.jar"