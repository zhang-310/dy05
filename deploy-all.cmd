@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo.
echo ============================================
echo   Gaifan Ops — 一键上线脚本 [DEPRECATED]
echo ============================================
echo.
echo [注意] gaifan-ops 外部基座已废弃。请使用 dy05 独立 prod 栈:
echo   pwsh scripts/start-gaifan-prod-stack.ps1
echo   见 docs/deployment/gaifan-prod-runbook.md
echo.
pause
exit /b 0

:: --- 以下为历史 gaifan-ops 流程，保留备查 ---

:: 检查 gaifan-ops 目录
if not exist "D:\gaifan\gaifan-ops\pom.xml" (
    echo [错误] 未找到 gaifan-ops 项目
    echo 请确认项目在 D:\gaifan\gaifan-ops
    exit /b 1
)

echo ============================================
echo   dy05 抖音运营 + gaifan-ops 多应用基座
echo ============================================
echo.

:: === dy05 部署 ===
echo ======== dy05 抖音运营平台 ========
if exist "pom.xml" (
    echo [dy05] 检测到 dy05 项目,开始部署...
    call deploy.cmd
) else (
    echo [dy05] 未找到 dy05 项目,跳过
)

:: === gaifan-ops 部署 ===
echo.
echo ======== gaifan-ops 多应用基座 ========

cd /d D:\gaifan\gaifan-ops

:: 复制配置
if not exist .env (
    if exist .env.example (
        copy .env.example .env >nul
    )
)

:: 编译
echo [gaifan-ops] 编译后端...
call mvn install -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [错误] gaifan-ops 编译失败!
    exit /b 1
)

:: 构建前端
echo [gaifan-ops] 构建前端...
cd front
if not exist node_modules (
    call npm install --silent
)
call npm run build >nul 2>&1
cd ..

:: 启动 Docker
echo [gaifan-ops] 启动 Docker (全部 7 个产品)...
docker compose -f docker-compose.all-in-one.yml up --build -d

:: 验证
echo [gaifan-ops] 等待服务就绪...
timeout /t 10 /nobreak >nul
curl -s http://localhost:8088/api/health >nul 2>&1
if %ERRORLEVEL% equ 0 (echo [OK] gaifan-ops 就绪) else (echo [警告] 请等待 30 秒让服务启动)

cd /d C:\claude\dy05

echo.
echo ============================================
echo   全部上线完成!
echo ============================================
echo.
echo   === dy05 ===
echo   后端:   http://localhost:8080
echo   前端:   http://localhost:3000
echo.
echo   === gaifan-ops ===
echo   统一入口: http://localhost:8088
echo   官网首页: http://localhost:8088/#/official
echo   管理后台: http://localhost:8088/#/dashboard
echo   后端 API:  http://localhost:18081/api/health
echo.
echo   === 产品一览 ===
echo   抖音运营: http://localhost:8088/#/douyin
echo   视频拆解: http://localhost:8088/#/collection
echo   数字人:   http://localhost:8088/#/digital-human
echo   照片转视频: http://localhost:8088/#/photo-avatar
echo   短剧制作: http://localhost:8088/#/drama
echo   短视频成片: http://localhost:8088/#/shortvideo-maker
echo   知识库:   http://localhost:8088/#/knowledge
echo.
