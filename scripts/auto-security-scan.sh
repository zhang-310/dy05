#!/bin/bash
# 安全漏洞扫描脚本
# 执行频率：每日 03:30
# 输出：reports/security/YYYY-MM-DD.json

set -e

DATE=$(date +%Y-%m-%d)
REPORT_DIR="reports/security"
mkdir -p "$REPORT_DIR"

echo "=========================================="
echo "安全漏洞扫描 - $DATE"
echo "=========================================="

# 依赖漏洞扫描（已在 auto-check-dependencies.sh 中执行）
echo ""
echo "=== 1/4 依赖漏洞扫描 ==="
echo "运行 OWASP Dependency-Check..."
mvn org.owasp:dependency-check-maven:check \
    -DfailBuildOnCVSS=7 \
    -q || echo "⚠️  发现依赖漏洞"

# 代码漏洞扫描
echo ""
echo "=== 2/4 代码漏洞扫描 ==="
echo "运行 SpotBugs Security..."
mvn spotbugs:check -q || echo "⚠️  发现代码漏洞"

# 容器漏洞扫描
echo ""
echo "=== 3/4 容器漏洞扫描 ==="
if command -v trivy &> /dev/null; then
    echo "运行 Trivy..."
    trivy image --format json --output "$REPORT_DIR/container-$DATE.json" \
        dy05-backend:latest || echo "⚠️  发现容器漏洞"
    echo "✅ 容器扫描报告: $REPORT_DIR/container-$DATE.json"
else
    echo "⚠️  Trivy 未安装，跳过容器扫描"
fi

# 汇总安全报告
echo ""
echo "=== 4/4 汇总安全报告 ==="
cat > "$REPORT_DIR/$DATE.json" <<EOF
{
  "date": "$DATE",
  "scans": {
    "dependencies": "$([ -f target/dependency-check-report.json ] && echo 'completed' || echo 'skipped')",
    "code": "$([ -f target/spotbugsXml.xml ] && echo 'completed' || echo 'skipped')",
    "container": "$([ -f $REPORT_DIR/container-$DATE.json ] && echo 'completed' || echo 'skipped')"
  }
}
EOF

echo "✅ 安全报告汇总: $REPORT_DIR/$DATE.json"

# 发送高危漏洞通知
if [ -f "scripts/notify-wecom.sh" ]; then
    # 检查是否有高危漏洞（简化版，实际需解析 JSON）
    if grep -q "CRITICAL\|HIGH" target/dependency-check-report.json 2>/dev/null; then
        ./scripts/notify-wecom.sh "⚠️ 发现高危安全漏洞，请查看报告: $REPORT_DIR/$DATE.json"
    fi
fi

echo ""
echo "=========================================="
echo "✅ 安全漏洞扫描完成"
echo "=========================================="
echo "报告位置: $REPORT_DIR/$DATE.json"
