# Post-M4 CI：平台单测 + 可选六产品 HTTP 验收（需 GAIFAN_VERIFY_BASE_URL 与运行中实例）
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [switch]$SkipHttpVerify,
    [switch]$FullSellable
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

Write-Host "=== run-six-products-ci === FullSellable=$FullSellable"
pwsh scripts/run-ai-foundation-verify.ps1
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not $SkipHttpVerify -and $BaseUrl) {
    if ($FullSellable) {
        Write-Host "[HTTP] verify-seven-products-sellable + feature-matrix against $BaseUrl"
        pwsh scripts/verify-seven-products-sellable.ps1 -BaseUrl $BaseUrl
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        pwsh scripts/verify-product-feature-matrix.ps1 -BaseUrl $BaseUrl
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        pwsh scripts/verify-product-integration-rules.ps1
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        if (Test-Path (Join-Path $PSScriptRoot "verify-product-integration-e2e.ps1")) {
            pwsh scripts/verify-product-integration-e2e.ps1 -BaseUrl $BaseUrl
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
    } else {
        Write-Host "[HTTP] verify-six-products-m4 against $BaseUrl"
        pwsh scripts/verify-six-products-m4.ps1 -BaseUrl $BaseUrl -SkipStaging
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        pwsh scripts/verify-product-knowledge-base.ps1 -BaseUrl $BaseUrl
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        if ($env:GAIFAN_JWT_E2E -eq "true") {
            pwsh scripts/verify-product-jwt-e2e.ps1 -BaseUrl $BaseUrl
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
    }
} else {
    Write-Host "[SKIP] HTTP verify (set GAIFAN_VERIFY_BASE_URL or pass -BaseUrl)"
}

Write-Host "[OK] run-six-products-ci passed"
