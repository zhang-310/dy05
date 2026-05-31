# P1 douyin-ops.video-analysis：进化爆款分析扣费
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

Write-Host "=== verify-product-douyin-ops-video-analysis ==="
$seed = Join-Path $PSScriptRoot "seed-gaifan-feature-verify.sql"
if (Test-Path $seed) {
    Get-Content $seed -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}
Q "delete from ai_viral_analysis where video_id=1;" | Out-Null
$trace = "verify-p1-video-analysis"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/ai/evolution/viral/trigger" `
    -Body @{ videoId = 1; accountId = 1 } -TenantId "demo-tenant" -UserId "1" -TraceId $trace -TimeoutSec 90
$ledger = Q "select count(*) from gf_credit_ledger where product_code='douyin-ops' and created_at > now() - interval '5 minutes';"
if ($r.http -ne 200 -or [int]$ledger -lt 1) {
    Write-Host "[FAIL] video-analysis http=$($r.http) ledger=$ledger body=$($r.body)"
    exit 1
}
Write-Host "[OK] verify-product-douyin-ops-video-analysis passed ledger=$ledger"
