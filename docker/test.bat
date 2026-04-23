@echo off
REM ============================================================
REM 抖音运营平台 - 部署测试脚本 (Windows)
REM ============================================================

echo ==========================================
echo 抖音运营平台 - 部署测试
echo ==========================================

set PASSED=0
set FAILED=0

echo.
echo 1. 检查 Docker 服务
echo ----------------------------------------
docker compose ps

echo.
echo 2. 端口连通性测试
echo ----------------------------------------

call :test_port "PostgreSQL" 5433
call :test_port "Redis" 6380
call :test_port "RabbitMQ" 5672
call :test_port "RabbitMQ 管理" 15672
call :test_port "Elasticsearch" 9200
call :test_port "后端应用" 8189
call :test_port "Nginx" 8888

echo.
echo 3. 服务健康检查
echo ----------------------------------------

call :test_url "后端健康检查" "http://localhost:8189/actuator/health"
call :test_url "Elasticsearch" "http://localhost:9200/_cluster/health"
call :test_url "前端页面" "http://localhost:8888"

echo.
echo 4. 数据库连接测试
echo ----------------------------------------

echo 测试数据库连接...
docker compose exec -T postgres psql -U postgres -d douyin_operations -c "SELECT 1" >nul 2>&1
if not errorlevel 1 (
    echo [OK] 数据库连接正常
    set /a PASSED+=1
) else (
    echo [FAIL] 数据库连接失败
    set /a FAILED+=1
)

echo.
echo 5. Redis 连接测试
echo ----------------------------------------

echo 测试 Redis 连接...
docker compose exec -T redis redis-cli ping | findstr "PONG" >nul 2>&1
if not errorlevel 1 (
    echo [OK] Redis 连接正常
    set /a PASSED+=1
) else (
    echo [FAIL] Redis 连接失败
    set /a FAILED+=1
)

echo.
echo 6. API 接口测试
echo ----------------------------------------

echo 测试健康检查接口...
curl -s http://localhost:8189/actuator/health | findstr "UP" >nul 2>&1
if not errorlevel 1 (
    echo [OK] 健康检查接口正常
    set /a PASSED+=1
) else (
    echo [FAIL] 健康检查接口失败
    set /a FAILED+=1
)

echo 测试 API 文档...
curl -f -s http://localhost:8189/v3/api-docs >nul 2>&1
if not errorlevel 1 (
    echo [OK] API 文档正常
    set /a PASSED+=1
) else (
    echo [FAIL] API 文档失败
    set /a FAILED+=1
)

echo.
echo 7. 容器资源使用
echo ----------------------------------------
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" dy-app dy-postgres dy-redis dy-rabbitmq dy-elasticsearch dy-nginx

echo.
echo ==========================================
echo 测试完成
echo ==========================================
echo 通过: %PASSED%
echo 失败: %FAILED%
echo.

if %FAILED% EQU 0 (
    echo [OK] 所有测试通过！系统运行正常
    exit /b 0
) else (
    echo [FAIL] 部分测试失败，请检查日志
    echo.
    echo 查看详细日志:
    echo   docker compose logs app
    echo   docker compose logs postgres
    echo   docker compose logs redis
    exit /b 1
)

:test_port
echo 测试 %~1 端口 %2...
netstat -an | findstr ":%2" >nul 2>&1
if not errorlevel 1 (
    echo [OK] %~1 端口正常
    set /a PASSED+=1
) else (
    echo [FAIL] %~1 端口未监听
    set /a FAILED+=1
)
goto :eof

:test_url
echo 测试 %~1...
curl -f -s "%~2" >nul 2>&1
if not errorlevel 1 (
    echo [OK] %~1 正常
    set /a PASSED+=1
) else (
    echo [FAIL] %~1 失败
    set /a FAILED+=1
)
goto :eof
