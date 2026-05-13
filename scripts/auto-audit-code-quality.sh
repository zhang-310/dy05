#!/bin/bash
# 代码质量自动审计脚本
# 执行频率：每日 03:00
# 输出：reports/code-quality/YYYY-MM-DD.{json,md}

set -e

DATE=$(date +%Y-%m-%d)
REPORT_DIR="reports/code-quality"
mkdir -p "$REPORT_DIR"

echo "=========================================="
echo "代码质量自动审计 - $DATE"
echo "=========================================="

# 后端代码质量检查
echo ""
echo "=== 1/5 后端代码质量检查 ==="
echo "运行 Checkstyle..."
mvn checkstyle:check -q || echo "⚠️  Checkstyle 发现问题"

echo "运行 SpotBugs..."
mvn spotbugs:check -q || echo "⚠️  SpotBugs 发现问题"

echo "运行 PMD..."
mvn pmd:check -q || echo "⚠️  PMD 发现问题"

echo "运行 SonarQube 分析..."
if [ -n "$SONAR_TOKEN" ]; then
    mvn sonar:sonar \
        -Dsonar.host.url=${SONAR_HOST_URL:-http://localhost:9000} \
        -Dsonar.token=$SONAR_TOKEN \
        -q
else
    echo "⚠️  SONAR_TOKEN 未配置，跳过 SonarQube 分析"
fi

# 前端代码质量检查
echo ""
echo "=== 2/5 前端代码质量检查 ==="
cd front

echo "运行 ESLint..."
npm run lint || echo "⚠️  ESLint 发现问题"

echo "运行 TypeScript 类型检查..."
npm run type-check || echo "⚠️  TypeScript 类型错误"

cd ..

# 生成 JSON 报告
echo ""
echo "=== 3/5 生成机器可读报告 ==="
if [ -f "scripts/generate-quality-report.py" ]; then
    python scripts/generate-quality-report.py > "$REPORT_DIR/$DATE.json"
    echo "✅ JSON 报告已生成: $REPORT_DIR/$DATE.json"
else
    echo "⚠️  generate-quality-report.py 不存在，跳过"
fi

# 生成 Markdown 报告
echo ""
echo "=== 4/5 生成人类可读报告 ==="
if [ -f "scripts/format-quality-report.py" ] && [ -f "$REPORT_DIR/$DATE.json" ]; then
    python scripts/format-quality-report.py "$REPORT_DIR/$DATE.json" > "$REPORT_DIR/$DATE.md"
    echo "✅ Markdown 报告已生成: $REPORT_DIR/$DATE.md"
else
    echo "⚠️  跳过 Markdown 报告生成"
fi

# 创建 GitHub Issue（P0 问题）
echo ""
echo "=== 5/5 创建 GitHub Issue ==="
if [ -f "scripts/create-issues-from-report.py" ] && [ -f "$REPORT_DIR/$DATE.json" ]; then
    python scripts/create-issues-from-report.py "$REPORT_DIR/$DATE.json"
    echo "✅ GitHub Issue 已创建"
else
    echo "⚠️  跳过 Issue 创建"
fi

echo ""
echo "=========================================="
echo "✅ 代码质量审计完成"
echo "=========================================="
echo "报告位置: $REPORT_DIR/$DATE.{json,md}"
