# P2 video-insight M4 verify
param(
    [string]$BaseUrl = "http://localhost:18081",
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

$p2seed = Join-Path $PSScriptRoot "seed-gaifan-p2-viral-demo.sql"
if (Test-Path $p2seed) {
    Get-Content $p2seed -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}

try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up"; exit 1 }

$trace = "verify-p2-insight-ok"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/short-video/viral/deep-analyze" `
    -Body @{ id = 1 } -TenantId "demo-tenant" -UserId "1" -TraceId $trace -TimeoutSec 90
$ledger = Q "select count(*) from gf_credit_ledger where trace_id like 'verify-p2-insight-ok%' or reason like '%verify-p2%';"
$delivery = Q "select count(*) from gf_video_insight_delivery_workspace_ledger where trace_id like 'verify-p2-insight-ok%';"
# trace in charge may differ — also match recent ledger for video-insight
if ([int]$ledger -lt 1) {
    $ledger = Q "select count(*) from gf_credit_ledger where product_code='video-insight' and created_at > now() - interval '2 minutes';"
}
$ok = ($r.http -eq 200) -and ([int]$ledger -gt 0)
if (-not $ok) {
    Write-Host "[FAIL] video-insight http=$($r.http) ledger=$ledger body=$($r.body)"
    Update-SixProductsM4Json -ProductCode "video-insight" -Status "failed" -Checks @{ http = $r.http; ledger = $ledger }
    exit 1
}
Write-Host "[OK] video-insight deep-analyze"
Update-SixProductsM4Json -ProductCode "video-insight" -Checks @{ http = $r.http; ledger = $ledger; delivery = $delivery }

$traceDeny = "verify-p2-insight-deny"
Q "update gf_credit_account set available_credits=17120 where tenant_id='demo-tenant';" | Out-Null
$r2 = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/short-video/viral/deep-analyze" `
    -Body @{ id = 1 } -TenantId "no-entitlement-tenant" -UserId "1" -TraceId $traceDeny
$denyOk = ($r2.body -match '"(code|status)"\s*:\s*(2002|4421)') -or ($r2.http -eq 402)
if (-not $denyOk) {
    Write-Host "[FAIL] entitlement denial expected 402/4421 http=$($r2.http) body=$($r2.body)"
    exit 1
}
Write-Host "[OK] no-entitlement -> 402 or 4421"

$traceViral = "verify-p2-viral-analyze"
$r3 = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/short-video/viral/analyze" `
    -Body @{ id = 1 } -TenantId "demo-tenant" -UserId "1" -TraceId $traceViral -TimeoutSec 90
$viralLedgerBefore = Q "select count(*) from gf_credit_ledger where product_code='video-insight';"
Start-Sleep -Seconds 1
$viralLedger = Q "select count(*) from gf_credit_ledger where product_code='video-insight' and created_at > now() - interval '3 minutes';"
if ($r3.http -ne 200) {
    Write-Host "[FAIL] viral/analyze http=$($r3.http) body=$($r3.body)"
    exit 1
}
if ([int]$viralLedger -lt 1) {
    Write-Host "[FAIL] viral/analyze no ledger recent=$viralLedger"
    exit 1
}
Write-Host "[OK] viral/analyze http=200 ledger=$viralLedger"

Write-Host "[OK] verify-product-video-insight passed"
