@echo off
REM ============================================================
REM 抖音运营平台 - 一键部署脚本 (Windows)
REM ============================================================

echo ==========================================
echo 抖音运营平台 - Docker 部署
echo ==========================================

REM 检查 Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo 错误: Docker 未安装
    exit /b 1
)

docker compose version >nul 2>&1
if errorlevel 1 (
    echo 错误: Docker Compose 未安装
    exit /b 1
)

REM 进入 docker 目录
cd /d "%~dp0"

REM 检查 .env 文件
if not exist .env (
    echo 创建 .env 文件...
    copy .env.example .env
    echo 请编辑 .env 文件配置数据库密码等信息
    echo 然后重新运行此脚本
    pause
    exit /b 0
)

echo.
echo 1. 构建镜像...
docker compose build

echo.
echo 2. 启动服务...
docker compose up -d

echo.
echo 3. 等待服务就绪...
timeout /t 10 /nobreak >nul

REM 检查数据库
echo 检查数据库连接...
for /l %%i in (1,1,30) do (
    docker compose exec -T postgres pg_isready -U postgres >nul 2>&1
    if not errorlevel 1 (
        echo 数据库已就绪
        goto :db_ready
    )
    echo 等待数据库启动... (%%i/30)
    timeout /t 2 /nobreak >nul
)
:db_ready

echo.
echo 4. 初始化数据库...
docker run --rm -v "%cd%\..\sql:/sql" --network docker_dy-net -e PGPASSWORD=postgresql postgres:15-alpine psql -h dy-postgres -U postgres -d douyin_operations -f /sql/init.sql

echo.
echo 5. 验证部署...

REM 检查后端健康
echo 检查后端服务...
for /l %%i in (1,1,30) do (
    curl -f http://localhost:8189/actuator/health >nul 2>&1
    if not errorlevel 1 (
        echo 后端服务正常
        goto :backend_ready
    )
    echo 等待后端启动... (%%i/30)
    timeout /t 2 /nobreak >nul
)
:backend_ready

REM 检查前端
echo 检查前端服务...
curl -I http://localhost:8888 >nul 2>&1
if not errorlevel 1 (
    echo 前端服务正常
) else (
    echo 警告: 前端服务未响应
)

echo.
echo ==========================================
echo 部署完成！
echo ==========================================
echo.
echo 访问地址:
echo   前端: http://localhost:8888
echo   后端: http://localhost:8189
echo   API 文档: http://localhost:8189/swagger-ui.html
echo   健康检查: http://localhost:8189/actuator/health
echo.
echo 管理界面:
echo   RabbitMQ: http://localhost:15672 (guest/guest)
echo.
echo 查看日志: docker compose logs -f app
echo 停止服务: docker compose down
echo.
pause
