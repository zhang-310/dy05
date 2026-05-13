#!/bin/bash
# 企业微信通知脚本
# 用法: ./notify-wecom.sh "消息内容"

MESSAGE="$1"

if [ -z "$MESSAGE" ]; then
    echo "用法: $0 <消息内容>"
    exit 1
fi

if [ -z "$WECOM_WEBHOOK" ]; then
    echo "⚠️  WECOM_WEBHOOK 未配置，跳过通知"
    exit 0
fi

curl -s -X POST "$WECOM_WEBHOOK" \
    -H 'Content-Type: application/json' \
    -d "{
        \"msgtype\": \"text\",
        \"text\": {
            \"content\": \"[DY05 自动化] $MESSAGE\"
        }
    }"

echo "✅ 企业微信通知已发送"
