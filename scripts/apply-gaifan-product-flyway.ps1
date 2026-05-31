# Post-M4：产品域 DDL（V209-V214）+ 商业化 V216-V224（Flyway 关闭环境）
param(
    [string]$PgContainer = "dy-postgres",
    [string]$DbPassword = "postgresql"
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$migrationDir = Join-Path $root "douyin-operations-app\src\main\resources\db\migration"
$productVersions = @("V209", "V210", "V211", "V212", "V213", "V214")

foreach ($ver in $productVersions) {
    $file = Get-ChildItem -Path $migrationDir -Filter "${ver}__*.sql" | Select-Object -First 1
    if (-not $file) { Write-Host "[SKIP] $ver"; continue }
    Write-Host "[APPLY] $($file.Name)"
    Get-Content $file.FullName -Raw | docker exec -i -e PGPASSWORD=$DbPassword $PgContainer `
        psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

& "$PSScriptRoot\apply-gaifan-commercial-migrations.ps1" -PgContainer $PgContainer -DbPassword $DbPassword

$seedSix = Join-Path $PSScriptRoot "seed-gaifan-six-products.sql"
if (Test-Path $seedSix) {
    Write-Host "[SEED] seed-gaifan-six-products.sql"
    Get-Content $seedSix -Raw | docker exec -i -e PGPASSWORD=$DbPassword $PgContainer `
        psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations
    if ($LASTEXITCODE -ne 0) { exit 1 }
}
$seedJwt = Join-Path $PSScriptRoot "seed-gaifan-jwt-tenant.sql"
if (Test-Path $seedJwt) {
    Write-Host "[SEED] seed-gaifan-jwt-tenant.sql"
    Get-Content $seedJwt -Raw | docker exec -i -e PGPASSWORD=$DbPassword $PgContainer `
        psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

$v221 = Join-Path $migrationDir "V221__gf_kb_vector_pgvector.sql"
if (Test-Path $v221) {
    Write-Host "[APPLY] V221 pgvector"
    Get-Content $v221 -Raw | docker exec -i -e PGPASSWORD=$DbPassword $PgContainer `
        psql -v ON_ERROR_STOP=0 -U postgres -d douyin_operations 2>&1 | Out-Host
}

$seedKb = Join-Path $PSScriptRoot "seed-gaifan-kb-demo.sql"
if (Test-Path $seedKb) {
    Write-Host "[SEED] seed-gaifan-kb-demo.sql"
    Get-Content $seedKb -Raw | docker exec -i -e PGPASSWORD=$DbPassword $PgContainer `
        psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

Write-Host "[OK] apply-gaifan-product-flyway completed"
