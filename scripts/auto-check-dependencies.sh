#!/bin/bash
# 依赖版本检查与安全扫描脚本
# 执行频率：每周日 04:00
# 输出：reports/dependencies/YYYY-MM-DD.json

set -e

DATE=$(date +%Y-%m-%d)
REPORT_DIR="reports/dependencies"
mkdir -p "$REPORT_DIR"

echo "=========================================="
echo "依赖版本检查 - $DATE"
echo "=========================================="

# 后端依赖检查
echo ""
echo "=== 1/5 后端依赖更新检查 ==="
mvn versions:display-dependency-updates \
    -DoutputFile="$REPORT_DIR/backend-updates-$DATE.txt" \
    -q

echo "✅ 后端依赖更新清单: $REPORT_DIR/backend-updates-$DATE.txt"

# 后端安全漏洞扫描
echo ""
echo "=== 2/5 后端安全漏洞扫描 ==="
mvn org.owasp:dependency-check-maven:check \
    -DfailBuildOnCVSS=7 \
    -DsuppressionFile=owasp-suppressions.xml \
    -q || echo "⚠️  发现安全漏洞"

if [ -f "target/dependency-check-report.json" ]; then
    cp target/dependency-check-report.json "$REPORT_DIR/backend-security-$DATE.json"
    echo "✅ 后端安全报告: $REPORT_DIR/backend-security-$DATE.json"
fi

# 前端依赖检查
echo ""
echo "=== 3/5 前端依赖更新检查 ==="
cd front

npx npm-check-updates --jsonUpgraded > "../$REPORT_DIR/frontend-updates-$DATE.json"
echo "✅ 前端依赖更新清单: $REPORT_DIR/frontend-updates-$DATE.json"

# 前端安全审计
echo ""
echo "=== 4/5 前端安全审计 ==="
npm audit --json > "../$REPORT_DIR/frontend-security-$DATE.json" || echo "⚠️  发现安全漏洞"
echo "✅ 前端安全报告: $REPORT_DIR/frontend-security-$DATE.json"

cd ..

# 自动升级安全补丁
echo ""
echo "=== 5/5 自动升级安全补丁 ==="
if [ -f "scripts/auto-upgrade-security-patches.py" ]; then
    python scripts/auto-upgrade-security-patches.py "$REPORT_DIR"
    echo "✅ 安全补丁已自动升级"
else
    echo "⚠️  auto-upgrade-security-patches.py 不存在，跳过自动升级"
fi

echo ""
echo "=========================================="
echo "✅ 依赖检查完成"
echo "=========================================="
echo "报告位置: $REPORT_DIR/*-$DATE.json"
