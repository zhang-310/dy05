@echo off
REM 设置抖音 cookies 环境变量
set YT_DLP_COOKIES_FILE=C:\secrets\douyin-cookies.txt
echo YT_DLP_COOKIES_FILE 已设置为: %YT_DLP_COOKIES_FILE%

REM 验证文件是否存在
if exist "%YT_DLP_COOKIES_FILE%" (
    echo ✓ Cookies 文件存在
) else (
    echo ✗ 警告: Cookies 文件不存在！
)
