# 以 UTF-8 编码执行 auth 资源初始化 SQL，避免中文乱码
# 用法: .\run-resource-data.ps1
# 注意：使用 docker cp 复制文件到容器内执行，避免 PowerShell 管道导致编码丢失
$sqlPath = Join-Path $PSScriptRoot "resource-data-v2.sql"
docker cp $sqlPath dy-postgres:/tmp/resource-data.sql
docker exec dy-postgres psql -U postgres -d douyin_operations -f /tmp/resource-data.sql
Write-Host "执行完成"
