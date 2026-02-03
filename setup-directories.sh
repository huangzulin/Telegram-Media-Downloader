#!/bin/bash

# 创建必要的目录
echo "Creating required directories..."
mkdir -p data downloads logs config

# 设置权限
chmod 755 data downloads logs config

echo "Directory structure created:"
ls -la