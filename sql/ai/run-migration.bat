@echo off
chcp 65001 >nul
REM ============================================================
REM AI 模块迁移脚本 (Windows)
REM 用法：项目根目录执行 run-ai-migration.bat 或 sql\ai\run-migration.bat [脚本名]
REM 建议用 run-ai-migration.bat 可避免中文乱码
REM ============================================================

setlocal
cd /d "%~dp0"

if "%~1"=="" (
  set "SQLFILE=migration-copy-processing-task.sql"
) else (
  set "SQLFILE=%~1"
)

if not exist "%SQLFILE%" (
  echo 错误: 找不到 %SQLFILE%
  exit /b 1
)

REM 优先用 Docker（dy-postgres 容器）
docker ps --filter "name=dy-postgres" --format "{{.Names}}" 2>nul | findstr /r "." >nul
if %errorlevel% equ 0 (
  echo === 通过 Docker 执行 %SQLFILE% ===
  type "%SQLFILE%" | docker exec -i dy-postgres psql -U postgres -d douyin_operations
  if %errorlevel% neq 0 exit /b 1
) else (
  if "%PGDATABASE%"=="" set PGDATABASE=douyin_operations
  if "%PGUSER%"=="" set PGUSER=postgres
  echo === 执行 %SQLFILE% ===
  psql -U %PGUSER% -d %PGDATABASE% -f "%SQLFILE%"
  if %errorlevel% neq 0 exit /b 1
)

echo === 迁移完成 ===
endlocal
