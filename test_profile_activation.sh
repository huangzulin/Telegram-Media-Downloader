#!/bin/bash
# GitHub Actions Maven Profile激活测试脚本

echo "🔍 测试GitHub Actions环境中的Maven Profile激活情况"

# 1. 显示系统信息
echo "=== 系统信息 ==="
uname -a
echo "Architecture: $(uname -m)"
echo "OS: $(cat /etc/os-release 2>/dev/null | grep PRETTY_NAME | cut -d'"' -f2)"

# 2. 检查Java版本
echo "=== Java信息 ==="
java -version

# 3. 检查Maven版本
echo "=== Maven信息 ==="
./mvnw --version

# 4. 测试自动Profile激活
echo "=== 自动Profile激活测试 ==="
echo "激活的Profiles:"
./mvnw help:active-profiles -q

# 5. 测试手动Profile激活
echo "=== 手动Profile激活测试 ==="
echo "使用-P linux-x64激活:"
./mvnw help:active-profiles -P linux-x64 -q

# 6. 检查依赖树
echo "=== 依赖树检查 ==="
echo "自动激活时的tdlight依赖:"
./mvnw dependency:tree -Dverbose | grep -i tdlight || echo "未找到tdlight依赖"

echo "手动激活linux-x64时的tdlight依赖:"
./mvnw dependency:tree -P linux-x64 -Dverbose | grep -i tdlight || echo "未找到tdlight依赖"

# 7. 构建测试
echo "=== 构建测试 ==="
echo "使用自动Profile构建:"
./mvnw clean compile -DskipTests -q && echo "✅ 自动Profile构建成功" || echo "❌ 自动Profile构建失败"

echo "使用手动Profile构建:"
./mvnw clean compile -P linux-x64 -DskipTests -q && echo "✅ 手动Profile构建成功" || echo "❌ 手动Profile构建失败"

# 8. 检查生成的JAR包
echo "=== JAR包内容检查 ==="
if [ -f "target/classes/it/tdlight" ] || [ -f "target/tmd-1.0.jar" ]; then
    echo "检查JAR包中的tdlight相关文件:"
    jar -tf target/tmd-1.0.jar 2>/dev/null | grep -i tdlight || echo "JAR包中未找到tdlight文件"
else
    echo "未找到生成的JAR包"
fi

echo "=== 测试完成 ==="
echo "建议: 如果自动Profile激活失败，应该使用手动Profile (-P linux-x64)"