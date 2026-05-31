# P7 knowledge-base.document：文档上传 + ledger
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

Write-Host "=== verify-product-knowledge-base-document ==="
$kbSeed = Join-Path $PSScriptRoot "seed-gaifan-kb-demo.sql"
if (Test-Path $kbSeed) {
    Get-Content $kbSeed -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}
$kbId = Q "select id from ai_knowledge_base where deleted=0 and user_id=1 order by id limit 1;"
$trace = "verify-kb-doc-upload"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/ai/knowledge-base/$kbId/document" `
    -Body @{ title = "verify upload"; content = "verify document upload content"; fileType = "txt" } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $trace -TimeoutSec 180
$ledger = Q "select count(*) from gf_credit_ledger where product_code='knowledge-base' and feature_code='knowledge-base.document' and created_at > now() - interval '5 minutes';"
if ($r.http -ne 200 -or [int]$ledger -lt 1) {
    Write-Host "[FAIL] kb document http=$($r.http) ledger=$ledger"
    exit 1
}
Write-Host "[OK] verify-product-knowledge-base-document passed ledger=$ledger"
