# Prod 七产品 smoke + feature-matrix 归档签字
param(
    [Parameter(Mandatory = $true)]
    [string]$ProdBaseUrl,
    [string]$PgContainer = "dy-postgres",
    [switch]$SkipFlyway
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

if (-not $SkipFlyway) {
    pwsh scripts/apply-gaifan-product-flyway.ps1 -PgContainer $PgContainer
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$env:GAIFAN_VERIFY_BASE_URL = $ProdBaseUrl.TrimEnd('/')
$env:KB_STORE = "pgvector"

Write-Host "=== Prod sign-off: $env:GAIFAN_VERIFY_BASE_URL ==="
pwsh scripts/verify-prod-smoke.ps1
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Copy-Item reports/automation/seven-products-sellable.json `
    reports/automation/seven-products-sellable-prod.json -Force
Copy-Item reports/automation/seven-products-feature-matrix.json `
    reports/automation/seven-products-feature-matrix-prod.json -Force

Write-Host "[OK] archived seven-products-sellable-prod.json + seven-products-feature-matrix-prod.json"
Write-Host "Update docs/deployment/prod-release-checklist.md prod column with date and operator."
