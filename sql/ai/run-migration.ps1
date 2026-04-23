# AI 模块迁移脚本 (PowerShell) - 解决中文乱码
# 用法：在项目根目录执行 .\sql\ai\run-migration.ps1 [脚本名]

chcp 65001 | Out-Null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$sqlFile = if ($args[0]) { $args[0] } else { "migration-copy-processing-task.sql" }

if (-not (Test-Path $sqlFile)) {
    Write-Host "错误: 找不到 $sqlFile"
    exit 1
}

$dockerRunning = docker ps --filter "name=dy-postgres" --format "{{.Names}}" 2>$null
if ($dockerRunning) {
    Write-Host "=== 通过 Docker 执行 $sqlFile ==="
    cmd /c "chcp 65001 >nul & type `"$scriptDir\$sqlFile`" | docker exec -i dy-postgres psql -U postgres -d douyin_operations"
    if ($LASTEXITCODE -ne 0) { exit 1 }
} else {
    $env:PGDATABASE = if ($env:PGDATABASE) { $env:PGDATABASE } else { "douyin_operations" }
    $env:PGUSER = if ($env:PGUSER) { $env:PGUSER } else { "postgres" }
    Write-Host "=== 执行 $sqlFile ==="
    psql -U $env:PGUSER -d $env:PGDATABASE -f $sqlFile
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

Write-Host "=== 迁移完成 ==="
