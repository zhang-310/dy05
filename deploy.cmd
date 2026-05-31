@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo.
echo ============================================
echo   dy05 抖音运营平台 — 一键上线脚本
echo ============================================
echo.

:: 检查必要工具
where java >nul 2>&1 || (echo [错误] 未找到 Java 17+. 请安装 JDK 17 或更高版本. && exit /b 1)
where docker >nul 2>&1 || (echo [警告] 未找到 Docker. 将仅编译,不启动容器. && set SKIP_DOCKER=1)
where node >nul 2>&1 || (echo [警告] 未找到 Node.js. 将跳过前端构建. && set SKIP_FRONTEND=1)

:: === 步骤 1: 后端编译 ===
echo.
echo [1/4] 编译后端 (Maven)...
call mvn install -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [错误] 后端编译失败!
    echo 尝试排查: mvn compile
    exit /b 1
)
echo [OK] 后端编译成功

:: === 步骤 2: 前端构建 ===
if defined SKIP_FRONTEND (
    echo [2/4] 跳过前端构建 (未安装 Node.js)
) else (
    echo [2/4] 构建前端...
    cd front
    if not exist node_modules (
        echo       安装前端依赖...
        call npm install --silent
    )
    call npm run build >nul 2>&1
    if %ERRORLEVEL% neq 0 (
        echo [警告] 前端构建有警告,继续...
    )
    cd ..
    echo [OK] 前端构建完成
)

:: === 步骤 3: Docker 启动 ===
if defined SKIP_DOCKER (
    echo [3/4] 跳过 Docker 启动
    echo.
    echo 手动启动: mvn -pl douyin-operations-app -am spring-boot:run
) else (
    echo [3/4] 启动 Docker 服务...

    :: 复制 .env
    if not exist .env (
        if exist .env.example (
            echo       创建 .env (从 .env.example)...
            copy .env.example .env >nul
        )
    )

    :: 构建并启动（含 AI profile）
    docker compose -f docker/docker-compose.yml up -d --build
    echo [信息] AI Provider (DeepSeek) + 企微通知 已配置
    if %ERRORLEVEL% neq 0 (
        echo [错误] Docker 启动失败!
        echo 尝试: docker compose -f docker/docker-compose.yml up --build
        echo 或直接 Java 启动: mvn -pl douyin-operations-app -am spring-boot:run
        exit /b 1
    )
    echo [OK] Docker 服务已启动
)

:: === 步骤 4: 验证 ===
echo [4/4] 验证服务...

:: 等待后端启动
echo       等待后端就绪 (最多 60 秒)...
set TRIES=0
:wait_loop
timeout /t 3 /nobreak >nul
set /a TRIES+=1
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %ERRORLEVEL% equ 0 goto health_ok
if %TRIES% lss 20 goto wait_loop
echo [警告] 后端未在 60 秒内就绪
goto done

:health_ok
echo [OK] 后端健康检查通过

:done
echo.
echo ============================================
echo   上线完成!
echo ============================================
echo.
echo   后端 API:  http://localhost:8080
echo   Swagger:   http://localhost:8080/swagger-ui.html
echo   前端页面:  http://localhost:3000
echo   健康检查:  http://localhost:8080/actuator/health
echo.
echo   查看日志:  docker compose -f docker/docker-compose.yml logs -f
echo   停止服务:  docker compose -f docker/docker-compose.yml down
echo   微服务模式: docker compose -f docker-compose.microservices.yml up -d
echo.
