@echo off
REM ========================================
REM Douyin Operations Platform Startup Script
REM ========================================

echo.
echo ========================================
echo   Douyin Operations Platform
echo ========================================
echo.

REM Set yt-dlp cookies environment variable
set YT_DLP_COOKIES_FILE=C:\secrets\douyin-cookies.txt

REM Verify cookies file
if exist "%YT_DLP_COOKIES_FILE%" (
    echo [OK] Cookies file configured: %YT_DLP_COOKIES_FILE%
) else (
    echo [WARNING] Cookies file not found: %YT_DLP_COOKIES_FILE%
    echo     Douyin video download may fail!
    echo.
)

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found, please install JDK 17
    pause
    exit /b 1
)

echo [OK] Java environment detected
echo.

REM Check application JAR file
set APP_JAR=douyin-operations-app\target\douyin-operations-app.jar
if not exist "%APP_JAR%" (
    echo [ERROR] Application JAR not found: %APP_JAR%
    echo     Please run: mvn clean package -DskipTests
    pause
    exit /b 1
)

echo [OK] Application file: %APP_JAR%
echo.

REM Set JVM parameters
set JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC

echo ========================================
echo   Starting application...
echo ========================================
echo.
echo Environment variables:
echo   YT_DLP_COOKIES_FILE=%YT_DLP_COOKIES_FILE%
echo.
echo JVM options:
echo   %JAVA_OPTS%
echo.
echo Application port: 8080
echo Frontend URL: http://localhost:3000
echo.
echo Press Ctrl+C to stop
echo ========================================
echo.

REM Start application
java %JAVA_OPTS% -jar "%APP_JAR%"

pause
