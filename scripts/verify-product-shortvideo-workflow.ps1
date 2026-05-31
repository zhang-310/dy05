# P3 workflow reserve/commit：数字人带货工作流入口
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")
. (Join-Path $PSScriptRoot "lib\New-GaifanDailyProjectId.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

Write-Host "=== verify-product-shortvideo-workflow ==="
$projectId = New-GaifanDailyProjectId -BaseUrl $BaseUrl -TraceId "verify-p3-wf-project"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/short-video/workflow/digital-human-commerce/start" `
    -Body @{ projectId = $projectId; scriptContent = "verify workflow" } `
    -TenantId "demo-tenant" -UserId "1" -TraceId "verify-p3-wf-start" -TimeoutSec 60
if ($r.http -ne 200) {
    Write-Host "[FAIL] workflow start http=$($r.http) body=$($r.body)"
    exit 1
}
$reservation = Q "select count(*) from gf_credit_reservation where business_key like 'project-$projectId%' and status in ('RESERVED','COMMITTED') and created_at > now() - interval '5 minutes';"
if ([int]$reservation -lt 1) {
    $reservation = Q "select count(*) from gf_credit_reservation where created_at > now() - interval '5 minutes';"
}
if ([int]$reservation -lt 1) {
    Write-Host "[FAIL] workflow reservation=$reservation"
    exit 1
}
Write-Host "[OK] verify-product-shortvideo-workflow passed reservation=$reservation"
