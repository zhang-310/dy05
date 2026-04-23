@echo off
chcp 65001 >nul
set YT_DLP_COOKIES_FILE=C:\secrets\douyin-cookies.txt

echo Cookies file: %YT_DLP_COOKIES_FILE%

if exist "%YT_DLP_COOKIES_FILE%" (
    echo [OK] Cookies file exists
) else (
    echo [WARNING] Cookies file not found
)

echo.
echo Starting application...
echo.

cd /d %~dp0
mvn -pl douyin-operations-app -am spring-boot:run

pause
