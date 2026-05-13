#!/bin/bash
# 测试覆盖率检查脚本
# 执行频率：每次 git push
# 输出：reports/coverage/YYYY-MM-DD.{html,json}

set -e

DATE=$(date +%Y-%m-%d)
REPORT_DIR="reports/coverage"
mkdir -p "$REPORT_DIR"

echo "=========================================="
echo "测试覆盖率检查 - $DATE"
echo "=========================================="

# 后端测试覆盖率
echo ""
echo "=== 1/4 后端测试覆盖率 ==="
mvn clean test jacoco:report -q

if [ -f "target/site/jacoco/index.html" ]; then
    cp target/site/jacoco/index.html "$REPORT_DIR/backend-$DATE.html"
    echo "✅ 后端覆盖率报告: $REPORT_DIR/backend-$DATE.html"
fi

# 前端测试覆盖率
echo ""
echo "=== 2/4 前端测试覆盖率 ==="
cd front
npm run test:coverage

if [ -f "coverage/index.html" ]; then
    cp coverage/index.html "../$REPORT_DIR/frontend-$DATE.html"
    echo "✅ 前端覆盖率报告: $REPORT_DIR/frontend-$DATE.html"
fi

cd ..

# 生成覆盖率汇总报告
echo ""
echo "=== 3/4 生成覆盖率汇总 ==="
if [ -f "scripts/generate-coverage-report.py" ]; then
    python scripts/generate-coverage-report.py > "$REPORT_DIR/$DATE.json"
    echo "✅ 覆盖率汇总: $REPORT_DIR/$DATE.json"
else
    echo "⚠️  generate-coverage-report.py 不存在，跳过"
fi

# 检查覆盖率阈值
echo ""
echo "=== 4/4 检查覆盖率阈值 ==="
if [ -f "scripts/check-coverage-threshold.py" ] && [ -f "$REPORT_DIR/$DATE.json" ]; then
    python scripts/check-coverage-threshold.py "$REPORT_DIR/$DATE.json"
else
    echo "⚠️  跳过阈值检查"
fi

echo ""
echo "=========================================="
echo "✅ 测试覆盖率检查完成"
echo "=========================================="
echo "报告位置: $REPORT_DIR/*-$DATE.html"
