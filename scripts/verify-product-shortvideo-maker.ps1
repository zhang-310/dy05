# P3 shortvideo-maker M4 verify（动态 project，不硬依赖 90001）
param(
    [string]$BaseUrl = "http://localhost:18081",
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")
. (Join-Path $PSScriptRoot "lib\New-GaifanDailyProjectId.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

$p3 = Join-Path $PSScriptRoot "seed-gaifan-p3-project-demo.sql"
if (Test-Path $p3) {
    Get-Content $p3 -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}

try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up"; exit 1 }

$projectId = New-GaifanDailyProjectId -BaseUrl $BaseUrl -TraceId "verify-p3-create-project"
Write-Host "[OK] dynamic projectId=$projectId"

$trace = "verify-p3-maker-export"
Q "update gf_credit_reservation set status='RELEASED', updated_at=now() where business_key='project-$projectId' and status='RESERVED';" | Out-Null
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/short-video/project/export-script" `
    -Body @{ projectId = $projectId } -TenantId "demo-tenant" -UserId "1" -TraceId $trace
$reservation = Q "select count(*) from gf_credit_reservation where trace_id like 'sv-export-$projectId%' or trace_id='$trace';"
$ledger = Q "select count(*) from gf_credit_ledger where trace_id like 'sv-export-$projectId%' or (product_code='shortvideo-maker' and created_at > now() - interval '2 minutes');"
$delivery = Q "select count(*) from gf_shortvideo_maker_delivery_workspace_ledger where trace_id like 'sv-export-$projectId%';"
$rules = Q "select count(*) from gf_product_integration_rule where rule_code='shortvideo-maker-to-photo-avatar';"
$ok = ($r.http -eq 200) -and (([int]$reservation -gt 0) -or ([int]$ledger -gt 0)) -and ([int]$delivery -gt 0) -and ([int]$rules -ge 1)
if (-not $ok) {
    Write-Host "[FAIL] shortvideo-maker http=$($r.http) reservation=$reservation ledger=$ledger delivery=$delivery rules=$rules"
    Update-SixProductsM4Json -ProductCode "shortvideo-maker" -Status "failed" -Checks @{ http = $r.http; projectId = $projectId }
    exit 1
}
Write-Host "[OK] shortvideo-maker export projectId=$projectId"
Update-SixProductsM4Json -ProductCode "shortvideo-maker" -Checks @{ http = $r.http; reservation = $reservation; ledger = $ledger; delivery = $delivery; rules = $rules; projectId = $projectId }
Write-Host "[OK] verify-product-shortvideo-maker passed"
