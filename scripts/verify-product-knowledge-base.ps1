# P7 knowledge-base：RAG search 必过（无 product/check 降级）
param(
    [string]$BaseUrl = "http://localhost:18081",
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanJwtApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

Write-Host "=== verify-product-knowledge-base ==="

$kbSeed = Join-Path $PSScriptRoot "seed-gaifan-kb-demo.sql"
if (Test-Path $kbSeed) {
    Get-Content $kbSeed -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}

try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up"; exit 1 }

$kbId = Q "select coalesce((select id from ai_knowledge_base where deleted=0 and user_id=1 order by id limit 1), 90001);"
$trace = "verify-kb-rag-ok"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Method POST `
    -Path "/api/v1/ai/knowledge-base/$kbId/search" `
    -Body @{ query = "verify rag"; topK = 3; queryRewrite = $false } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $trace -TimeoutSec 180

if ($r.http -ne 200) {
    Write-Host "[FAIL] kb rag search http=$($r.http) body=$($r.body)"
    Update-SevenProductsSellableJson -ProductCode "knowledge-base" -Status "failed" -Checks @{ http = $r.http; mode = "rag_search" }
    exit 1
}

$ledger = Q "select count(*) from gf_credit_ledger where product_code='knowledge-base' and created_at > now() - interval '10 minutes';"
if ([int]$ledger -lt 1) {
    Write-Host "[FAIL] kb rag ledger=$ledger"
    exit 1
}
Write-Host "[OK] knowledge-base rag_search http=200 ledger=$ledger"

$jwtSeed = Join-Path $PSScriptRoot "seed-gaifan-jwt-tenant.sql"
if (Test-Path $jwtSeed) {
    Get-Content $jwtSeed -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}
$token = Get-GaifanJwtToken -BaseUrl $BaseUrl
$kbIdJwt = Q "select id from ai_knowledge_base where deleted=0 and user_id=1 order by id limit 1;"
$jwtR = Invoke-GaifanJwtApi -BaseUrl $BaseUrl -Token $token -Path "/api/v1/ai/knowledge-base/$kbIdJwt/search" `
    -Body @{ query = "verify rag jwt"; topK = 3; queryRewrite = $false } -TraceId "verify-kb-jwt-search"
if ($jwtR.http -ne 200) {
    Write-Host "[FAIL] kb JWT search http=$($jwtR.http)"
    exit 1
}
Write-Host "[OK] knowledge-base JWT search http=$($jwtR.http)"
Update-SevenProductsSellableJson -ProductCode "knowledge-base" -Checks @{ http = 200; mode = "rag_search"; ledger = $ledger; jwt = $true }
Write-Host "[OK] verify-product-knowledge-base passed"
