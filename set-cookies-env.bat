@echo off
chcp 65001 >nul

echo Setting YT_DLP_COOKIES_FILE environment variable...
echo.

REM Set system environment variable (requires admin)
setx YT_DLP_COOKIES_FILE "C:\secrets\douyin-cookies.txt" /M

if errorlevel 1 (
    echo [ERROR] Failed to set system variable. Please run as Administrator.
    echo.
    echo Alternative: Set user variable instead
    setx YT_DLP_COOKIES_FILE "C:\secrets\douyin-cookies.txt"
)

echo.
echo [OK] Environment variable set successfully
echo.
echo Please restart your application to apply changes.
echo.

pause
