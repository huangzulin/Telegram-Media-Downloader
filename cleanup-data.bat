@echo off
echo 正在清理所有下载数据...

echo.
echo 1. 停止正在运行的应用程序...
taskkill /f /im java.exe 2>nul

echo.
echo 2. 删除数据库文件...
del /q data\tmd.db 2>nul
echo 数据库文件已删除

echo.
echo 3. 删除下载文件...
rd /s /q downloads 2>nul
mkdir downloads
mkdir downloads\videos
mkdir downloads\thumbnails
mkdir downloads\temp
echo 下载目录已清理并重新创建

echo.
echo 4. 创建新的空数据库...
echo. > data\tmd.db
echo 空数据库文件已创建

echo.
echo 数据清理完成！
echo 现在可以重新启动应用程序
pause