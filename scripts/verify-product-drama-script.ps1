# P6 drama-ai.script：update 写入扣费
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

Write-Host "=== verify-product-drama-script ==="
try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up"; exit 1 }

$projectId = Q "select id from drama_project where deleted=0 order by id desc limit 1;"
if (-not $projectId) {
    $cr = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/drama/create" `
        -Body @{ title = "verify drama script"; genre = "comedy"; episodeCount = 1 } `
        -TenantId "demo-tenant" -UserId "1" -TraceId "verify-drama-script-create"
    if ($cr.http -ne 200) { Write-Host "[FAIL] drama create http=$($cr.http)"; exit 1 }
    $projectId = Q "select id from drama_project where deleted=0 order by id desc limit 1;"
}

$before = Q "select count(*) from gf_credit_ledger where product_code='drama-ai' and feature_code='drama-ai.script';"
$trace = "verify-drama-script-$(Get-Date -Format 'HHmmss')"
$ru = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/drama/update" `
    -Body @{ id = [int]$projectId; script = "verify drama script charge $trace" } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $trace
$after = Q "select count(*) from gf_credit_ledger where product_code='drama-ai' and feature_code='drama-ai.script';"
if ($ru.http -ne 200 -or [int]$after -le [int]$before) {
    Write-Host "[FAIL] drama script update http=$($ru.http) ledger before=$before after=$after"
    exit 1
}
Write-Host "[OK] verify-product-drama-script passed ledger=$after"
