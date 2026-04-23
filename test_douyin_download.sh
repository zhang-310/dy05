#!/bin/bash
# 测试抖音视频下载（使用 cookies）

COOKIES_FILE="C:/secrets/douyin-cookies.txt"
TEST_URL="https://www.douyin.com/video/7123456789012345678"  # 示例 URL

echo "=== 测试抖音下载配置 ==="
echo ""
echo "1. Cookies 文件检查:"
if [ -f "$COOKIES_FILE" ]; then
    echo "   ✓ 文件存在: $COOKIES_FILE"
    echo "   大小: $(ls -lh "$COOKIES_FILE" | awk '{print $5}')"
    echo "   前 3 行:"
    head -3 "$COOKIES_FILE" | sed 's/^/     /'
else
    echo "   ✗ 文件不存在: $COOKIES_FILE"
    exit 1
fi

echo ""
echo "2. yt-dlp 版本:"
echo "   $(yt-dlp --version)"

echo ""
echo "3. 测试命令（不实际下载）:"
echo "   yt-dlp --cookies \"$COOKIES_FILE\" --dump-json <抖音URL>"

echo ""
echo "=== 配置完成 ==="
echo ""
echo "下一步："
echo "1. 设置环境变量: export YT_DLP_COOKIES_FILE=\"$COOKIES_FILE\""
echo "2. 重启应用"
echo "3. 在前端触发拆解分析"
