@echo off
REM TDLight原生库加载问题修复脚本 (Windows)

setlocal enabledelayedexpansion

echo 🔧 修复TDLight原生库加载问题...

REM 检测当前平台
set PLATFORM=%PROCESSOR_ARCHITECTURE%
echo 检测到平台: Windows-%PLATFORM%

REM 根据平台设置MAVEN_PROFILE
if "%PLATFORM%"=="AMD64" (
    set MAVEN_PROFILE=windows-x64
    echo 设置MAVEN_PROFILE为: %MAVEN_PROFILE%
) else (
    echo 不支持的架构: %PLATFORM%
    exit /b 1
)

REM 清理之前的构建
echo 🧹 清理之前的构建...
call mvnw.cmd clean

REM 使用正确的profile重新构建
echo 🏗️ 使用profile %MAVEN_PROFILE%重新构建...
call mvnw.cmd clean package -P%MAVEN_PROFILE% -DskipTests

if !errorlevel! equ 0 (
    echo ✅ 构建完成！TDLight原生库应该已正确加载。
    echo 现在可以运行: java -jar target/*.jar
) else (
    echo ❌ 构建失败，请检查错误信息
    exit /b 1
)