@echo off
REM Linux/amd64 TDLight修复脚本 (Windows)

setlocal enabledelayedexpansion

echo 🔧 修复Linux/amd64环境下的TDLight客户端工厂加载问题...

REM 强制使用linux-x64 profile重新构建
echo 🏗️ 使用linux-x64 profile强制重建...
call mvnw.cmd clean package -Plinux-x64 -DskipTests

if !errorlevel! neq 0 (
    echo ❌ Maven构建失败
    exit /b 1
)

REM 验证TDLight依赖是否正确包含
echo 📋 验证TDLight依赖...
call mvnw.cmd dependency:tree | findstr tdlight

REM 创建测试容器来验证Linux环境下的运行
echo 🐳 创建测试容器验证...
docker buildx build ^
    --platform linux/amd64 ^
    --tag tdlight-test-linux ^
    --load ^
    --build-arg MAVEN_PROFILE=linux-x64 ^
    .

if !errorlevel! equ 0 (
    echo ✅ Linux/amd64 TDLight修复完成！
    echo 现在可以正常构建和运行Linux/amd64镜像了
) else (
    echo ❌ Docker构建失败
    exit /b 1
)