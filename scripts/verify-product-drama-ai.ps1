# P6 drama-ai M4 verify
param(
    [string]$BaseUrl = "http://localhost:18081",
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up"; exit 1 }

$trace = "verify-p6-drama-ai"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/drama/create" `
    -Body @{ title = "M4 verify drama"; genre = "comedy"; episodeCount = 1 } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $trace
$ledger = Q "select count(*) from gf_credit_ledger where product_code='drama-ai' and created_at > now() - interval '2 minutes';"
$delivery = Q "select count(*) from gf_drama_ai_delivery_workspace_ledger where trace_id='$trace';"
$rules = Q "select count(*) from gf_product_integration_rule where rule_code='drama-ai-to-shortvideo-maker';"
$ok = ($r.http -eq 200) -and ([int]$ledger -gt 0) -and ([int]$delivery -gt 0) -and ([int]$rules -ge 1)
if (-not $ok) {
    Write-Host "[FAIL] drama-ai http=$($r.http) ledger=$ledger delivery=$delivery rules=$rules"
    Update-SixProductsM4Json -ProductCode "drama-ai" -Status "failed" -Checks @{ http = $r.http }
    exit 1
}
Write-Host "[OK] drama-ai create"
Update-SixProductsM4Json -ProductCode "drama-ai" -Checks @{ http = $r.http; ledger = $ledger; delivery = $delivery; rules = $rules }

$exportTrace = "verify-p6-drama-export"
$projectId = Q "select id from drama_project where deleted=0 order by id desc limit 1;"
$re = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/drama/export-to-maker" `
    -Body @{ id = [int]$projectId; traceId = $exportTrace } -TenantId "demo-tenant" -UserId "1" -TraceId $exportTrace
$invocation = Q "select count(*) from gf_product_integration_invocation where trace_id='$exportTrace';"
$makerDelivery = Q "select count(*) from gf_shortvideo_maker_delivery_workspace_ledger where trace_id='$exportTrace';"
$exportOk = ($re.http -eq 200) -and ([int]$invocation -gt 0) -and ([int]$makerDelivery -gt 0)
if (-not $exportOk) {
    Write-Host "[FAIL] drama export-to-maker http=$($re.http) invocation=$invocation delivery=$makerDelivery"
    exit 1
}
Write-Host "[OK] drama export-to-maker invocation=$invocation delivery=$makerDelivery"

$scriptTrace = "verify-p6-drama-script"
$scriptLedgerBefore = Q "select count(*) from gf_credit_ledger where product_code='drama-ai' and feature_code='drama-ai.script';"
$ru = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/drama/update" `
    -Body @{ id = [int]$projectId; script = "verify drama script content for charge" } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $scriptTrace
$scriptLedger = Q "select count(*) from gf_credit_ledger where product_code='drama-ai' and created_at > now() - interval '3 minutes';"
if ($ru.http -ne 200 -or ([int]$scriptLedger -le [int]$scriptLedgerBefore -and [int]$scriptLedger -lt 1)) {
    Write-Host "[FAIL] drama script update http=$($ru.http) ledger=$scriptLedger"
    exit 1
}
Write-Host "[OK] drama-ai.script ledger=$scriptLedger"
Write-Host "[OK] verify-product-drama-ai passed"
