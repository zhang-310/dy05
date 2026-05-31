# W1：douyin-ops 支付 HTTP 履约 → credits + delivery（confirmPayment）
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }

. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) {
    $out = docker exec -e PGPASSWORD=postgresql $PgContainer psql -U postgres -d douyin_operations -t -A -c $sql 2>&1
    if ($LASTEXITCODE -ne 0) { throw $out }
    return ($out | Out-String).Trim()
}

function Test-RestSuccess($body) {
    if ($body -match '"success"\s*:\s*true') { return $true }
    if ($body -match '"valid"\s*:\s*true') { return $true }
    if ($body -match '"status"\s*:\s*200[^0-9]') { return $true }
    return $false
}

Write-Host "=== verify-product-douyin-ops-payment-http === BaseUrl=$BaseUrl"
$seed = Join-Path $PSScriptRoot "seed-gaifan-payment-verify.sql"
if (Test-Path $seed) {
    Get-Content $seed -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}

$orderNo = "VERIFY-PAY-" + (Get-Date -Format "yyyyMMddHHmmss")
Q "insert into payment_order (order_no, user_id, owner_id, org_id, product_id, amount, actual_amount, quantity, status, deleted, created_at, updated_at)
   values ('$orderNo', 1, 1, 1, 1, 99, 99, 1, 'PENDING_PAYMENT', 0, current_timestamp, current_timestamp);" | Out-Null
$orderId = Q "select id from payment_order where order_no='$orderNo' limit 1;"
$txn = "txn-$orderNo"

$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/payment/order/confirmPayment" `
    -Body @{ orderId = [string]$orderId; transactionId = $txn; paymentMethod = "verify-http" } `
    -TenantId "demo-tenant" -UserId "1" -TraceId "payment-http-$orderNo"
if ($r.http -ne 200 -or -not (Test-RestSuccess $r.body)) {
    Write-Host "[FAIL] confirmPayment http=$($r.http) body=$($r.body)"
    exit 1
}

$ledger = Q "select count(*) from gf_credit_ledger where trace_id = 'payment-order-$orderId';"
$delivery = Q "select count(*) from gf_douyin_ops_delivery_workspace_ledger where trace_id = 'payment-order-$orderId';"
if ([int]$ledger -lt 1) {
    Write-Host "[FAIL] confirmPayment no credit ledger ledger=$ledger"
    exit 1
}
Write-Host "[OK] payment HTTP confirmPayment http=$($r.http) ledger=$ledger delivery=$delivery"
