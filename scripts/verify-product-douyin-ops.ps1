# P1 douyin-ops M4 verify
param(
    [string]$BaseUrl = "http://localhost:18081",
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up: $BaseUrl"; exit 1 }

$trace = "verify-p1-douyin-ops"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/ai/douyin-ops-commander/brief" -TenantId "demo-tenant" -UserId "1" -TraceId $trace -TimeoutSec 200
$ledger = Q "select count(*) from gf_credit_ledger where trace_id='$trace';"
$delivery = Q "select count(*) from gf_douyin_ops_delivery_workspace_ledger where trace_id='$trace';"
$ok = ($r.http -eq 200) -and ([int]$ledger -gt 0) -and ([int]$delivery -gt 0)
if (-not $ok) {
    Write-Host "[FAIL] douyin-ops http=$($r.http) ledger=$ledger delivery=$delivery"
    Update-SixProductsM4Json -ProductCode "douyin-ops" -Status "failed" -Checks @{ http = $r.http; ledger = $ledger; delivery = $delivery }
    exit 1
}
Write-Host "[OK] douyin-ops brief charge+delivery"
Update-SixProductsM4Json -ProductCode "douyin-ops" -Checks @{ http = $r.http; ledger = $ledger; delivery = $delivery }
Write-Host "[OK] verify-product-douyin-ops passed"
