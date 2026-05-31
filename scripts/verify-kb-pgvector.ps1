# 知识库 pgvector 前置检查（Wave4）
param(
    [switch]$AllowMissingExtension,
    [string]$PgContainer = "dy-postgres",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "postgresql",
    [string]$DbName = "douyin_operations",
    [string]$BaseUrl = "http://localhost:18082"
)

$ErrorActionPreference = "Stop"
& "$PSScriptRoot\migrate-kb-vectors-to-pg.ps1" -PgContainer $PgContainer -DbPassword $DbPassword -BaseUrl $BaseUrl
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$ext = docker exec -e PGPASSWORD=$DbPassword $PgContainer psql -U $DbUser -d $DbName -t -A `
    -c "select count(*) from pg_extension where extname='vector';"
if ([int]$ext -lt 1) {
    if ($AllowMissingExtension) {
        Write-Host "[SKIP] pgvector extension not on $PgContainer (PG15 alpine); use pgvector/pgvector:pg16 for prod"
        exit 0
    }
    Write-Host "[FAIL] pgvector extension not installed"
    exit 1
}
Write-Host "[OK] pgvector extension ready (set KB_STORE=pgvector and restart staging)"
