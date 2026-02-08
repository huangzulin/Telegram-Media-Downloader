Write-Host "正在清理所有下载数据..." -ForegroundColor Green

Write-Host "`n1. 停止正在运行的应用程序..." -ForegroundColor Yellow
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue

Write-Host "`n2. 删除数据库文件..." -ForegroundColor Yellow
Remove-Item "data\tmd.db" -Force -ErrorAction SilentlyContinue
Write-Host "数据库文件已删除"

Write-Host "`n3. 删除下载文件..." -ForegroundColor Yellow
Remove-Item "downloads" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "downloads" -Force | Out-Null
New-Item -ItemType Directory -Path "downloads\videos" -Force | Out-Null
New-Item -ItemType Directory -Path "downloads\thumbnails" -Force | Out-Null
New-Item -ItemType Directory -Path "downloads\temp" -Force | Out-Null
Write-Host "下载目录已清理并重新创建"

Write-Host "`n4. 创建新的空数据库..." -ForegroundColor Yellow
New-Item -ItemType File -Path "data\tmd.db" -Force | Out-Null
Write-Host "空数据库文件已创建"

Write-Host "`n数据清理完成！" -ForegroundColor Green
Write-Host "现在可以重新启动应用程序" -ForegroundColor Cyan
Read-Host "按回车键退出"