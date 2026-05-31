# W1：douyin-ops 支付平台 callback → credits + delivery
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$PgContainer = "dy-postgres",
    [string]$MerchantSecret = $env:DOUYIN_MERCHANT_SECRET
)
$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }
if (-not $MerchantSecret) { $MerchantSecret = "gaifan-staging-verify-secret" }

function Q($sql) {
    $out = docker exec -e PGPASSWORD=postgresql $PgContainer psql -U postgres -d douyin_operations -t -A -c $sql 2>&1
    if ($LASTEXITCODE -ne 0) { throw $out }
    return ($out | Out-String).Trim()
}

function New-PaymentCallbackSign([string]$OrderNo, [string]$Status, [string]$TransactionId, [string]$Amount, [string]$Secret) {
    # 与 DouyinPaymentService.verifySignature TreeMap 字典序一致：amount, orderId, status, transactionId
    $signPayload = "amount=$Amount&orderId=$OrderNo&status=$Status&transactionId=$TransactionId&key=$Secret"
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Secret))
    $hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($signPayload))
    return -join ($hash | ForEach-Object { $_.ToString("x2") })
}

Write-Host "=== verify-product-douyin-ops-payment-callback === BaseUrl=$BaseUrl"
$seed = Join-Path $PSScriptRoot "seed-gaifan-payment-verify.sql"
if (Test-Path $seed) {
    Get-Content $seed -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}

$orderNo = "VERIFY-CB-" + (Get-Date -Format "yyyyMMddHHmmss")
$amountForSign = "99"
Q "insert into payment_order (order_no, user_id, owner_id, org_id, product_id, amount, actual_amount, quantity, status, deleted, created_at, updated_at)
   values ('$orderNo', 1, 1, 1, 1, $amountForSign, $amountForSign, 1, 'PENDING_PAYMENT', 0, current_timestamp, current_timestamp);" | Out-Null
$txn = "txn-cb-$orderNo"
$sign = New-PaymentCallbackSign -OrderNo $orderNo -Status "SUCCESS" -TransactionId $txn -Amount $amountForSign -Secret $MerchantSecret

$body = @{
    orderId       = $orderNo
    status        = "SUCCESS"
    transactionId = $txn
    amount        = [int]$amountForSign
    sign          = $sign
} | ConvertTo-Json -Compress

$uri = "$BaseUrl/api/v1/payment/callback"
try {
    $r = Invoke-WebRequest -Uri $uri -Method POST -ContentType "application/json" -Body $body -UseBasicParsing -TimeoutSec 60
    $http = [int]$r.StatusCode
} catch {
    $http = 0
    if ($_.Exception.Response) { $http = [int]$_.Exception.Response.StatusCode }
    Write-Host "[FAIL] callback http=$http err=$($_.Exception.Message)"
    exit 1
}

$orderStatus = Q "select status from payment_order where order_no='$orderNo' limit 1;"
$ledger = Q "select count(*) from gf_credit_ledger where trace_id = 'payment-callback-$orderNo';"
$delivery = Q "select count(*) from gf_douyin_ops_delivery_workspace_ledger where trace_id = 'payment-callback-$orderNo';"
if ($http -ne 200) {
    Write-Host "[FAIL] callback http=$http"
    exit 1
}
if ($orderStatus -ne "PAID") {
    Write-Host "[FAIL] order not PAID status=$orderStatus"
    exit 1
}
if ([int]$ledger -lt 1 -and [int]$delivery -lt 1) {
    Write-Host "[FAIL] callback no ledger/delivery ledger=$ledger delivery=$delivery"
    exit 1
}
Write-Host "[OK] payment callback http=$http order=$orderStatus ledger=$ledger delivery=$delivery"
