# M4 六产品总验收
param(
    [string]$BaseUrl = "http://localhost:18081",
    [string]$PgContainer = "dy-postgres",
    [switch]$SkipUnitVerify,
    [switch]$SkipStaging
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

Write-Host "=== M4 six-products verify ==="

pwsh scripts/apply-gaifan-commercial-migrations.ps1 -PgContainer $PgContainer
if ($LASTEXITCODE -ne 0) { exit 1 }

pwsh scripts/verify-product-common.ps1 -PgContainer $PgContainer
if ($LASTEXITCODE -ne 0) { exit 1 }

$scripts = @(
    "verify-product-douyin-ops.ps1",
    "verify-product-video-insight.ps1",
    "verify-product-shortvideo-maker.ps1",
    "verify-product-digital-human.ps1",
    "verify-product-photo-avatar-video.ps1",
    "verify-product-drama-ai.ps1"
)
foreach ($s in $scripts) {
    Write-Host "--- $s ---"
    pwsh (Join-Path $PSScriptRoot $s) -BaseUrl $BaseUrl -PgContainer $PgContainer
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

pwsh (Join-Path $PSScriptRoot "verify-product-integration-rules.ps1") -PgContainer $PgContainer
if ($LASTEXITCODE -ne 0) { exit 1 }

if (-not $SkipUnitVerify) {
    pwsh scripts/run-ai-foundation-verify.ps1
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

if (-not $SkipStaging) {
    try {
        $h = Invoke-WebRequest -Uri "http://localhost:18082/actuator/health" -UseBasicParsing -TimeoutSec 3
        if ($h.StatusCode -eq 200) {
            pwsh scripts/run-gaifan-staging-regression.ps1
            if ($LASTEXITCODE -ne 0) { Write-Host "[WARN] staging regression failed"; exit 1 }
        }
    } catch {
        Write-Host "[SKIP] staging 18082 not running"
    }
}

Write-Host "[OK] verify-six-products-m4 passed"
