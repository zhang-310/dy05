# W1：douyin-ops 支付履约 — PaymentCreditGrantAdapter PG 集成 + douyin-ops delivery
param(
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

Write-Host "=== verify-product-douyin-ops-payment ==="
Write-Host "[TEST] PaymentCreditGrantAdapterPgTest (credit ledger + account)"
mvn -pl douyin-operations-app -Dtest=PaymentCreditGrantAdapterPgTest -DfailIfNoTests=false test -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] PaymentCreditGrantAdapterPgTest"
    exit 1
}

Write-Host "[TEST] PaymentCreditGrantAdapterDouyinOpsDeliveryTest"
mvn -pl douyin-operations-app -Dtest=PaymentCreditGrantAdapterDouyinOpsDeliveryTest -DfailIfNoTests=false test -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] PaymentCreditGrantAdapterDouyinOpsDeliveryTest"
    exit 1
}

Write-Host "[OK] verify-product-douyin-ops-payment passed (PG integration)"
