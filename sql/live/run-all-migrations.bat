@echo off
REM live 模块 - 一键执行所有迁移（Windows）
REM 用法：在项目根目录执行 sql\live\run-all-migrations.bat
REM 需配置：set PGHOST=localhost PGPORT=5432 PGUSER=postgres PGDATABASE=douyin_operations

setlocal
cd /d "%~dp0"

if "%PGDATABASE%"=="" set PGDATABASE=douyin_operations
if "%PGUSER%"=="" set PGUSER=postgres

echo === live 模块迁移开始 ===
for %%f in (migration-persona.sql migration-fields.sql migration-data-sync.sql migration-ai-analysis.sql migration-script-template.sql migration-monitor-fields.sql migration-ai-review.sql migration-monitor-archive.sql) do (
  echo --- 执行 %%f ---
  psql -U %PGUSER% -d %PGDATABASE% -f "%%f" || exit /b 1
)
echo === live 模块迁移完成 ===
endlocal
