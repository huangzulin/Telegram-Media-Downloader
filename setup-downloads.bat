@echo off
setlocal

echo 🔧 设置Telegram Media Downloader downloads目录...
echo.

REM 创建必要的目录结构
echo 📁 创建目录结构...
mkdir downloads\videos 2>nul
mkdir downloads\thumbnails 2>nul  
mkdir downloads\temp 2>nul
mkdir data 2>nul
mkdir logs 2>nul
mkdir config 2>nul

REM 设置目录权限（Windows下主要是确保目录存在）
echo 🔐 确保目录权限正确...

REM 在Windows上，我们主要确保目录存在并且可访问
REM Docker Desktop会处理容器内部的权限映射

echo ✅ 目录设置完成！
echo.
echo 📊 当前目录结构：
echo downloads\
echo ├── videos\      # 视频文件存储目录
echo ├── thumbnails\  # 视频缩略图目录
echo └── temp\        # 临时文件目录
echo.
echo 💡 Docker挂载说明：
echo - .\downloads 将挂载到容器内的 /app/downloads
echo - 下载的文件将在 downloads\videos\ 目录中
echo - 可以直接访问 http://localhost:3222/downloads/videos/ 查看文件
echo.
echo 🚀 下一步：
echo 1. 编辑 .env 文件配置Telegram API凭证
echo 2. 运行: docker-compose up -d
echo 3. 访问: http://localhost:3222
echo.
pause
