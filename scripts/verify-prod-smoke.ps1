# prod 发布前 smoke（staging 或 prod BaseUrl）
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [switch]$SkipJwt
)

$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }

Write-Host "=== verify-prod-smoke === BaseUrl=$BaseUrl"

pwsh (Join-Path $PSScriptRoot "verify-seven-products-sellable.ps1") -BaseUrl $BaseUrl -SkipJwt:$SkipJwt
if ($LASTEXITCODE -ne 0) { exit 1 }

if (-not $SkipJwt) {
    pwsh (Join-Path $PSScriptRoot "verify-product-jwt-e2e.ps1") -BaseUrl $BaseUrl
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

$env:GAIFAN_MATRIX_SKIP_M4 = "true"
pwsh (Join-Path $PSScriptRoot "verify-product-feature-matrix.ps1") -BaseUrl $BaseUrl
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "[OK] verify-prod-smoke passed"
