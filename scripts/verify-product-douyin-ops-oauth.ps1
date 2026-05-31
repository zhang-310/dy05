# P1 douyin-ops OAuth 扣费：CommercialProductChargeService 单测门禁
param()
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
Write-Host "=== verify-product-douyin-ops-oauth ==="
mvn -pl douyin-operations-app -Dtest=DouyinOAuthCallbackChargeTest -DfailIfNoTests=false test -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] DouyinOAuthCallbackChargeTest"
    exit 1
}
Write-Host "[OK] verify-product-douyin-ops-oauth passed"
