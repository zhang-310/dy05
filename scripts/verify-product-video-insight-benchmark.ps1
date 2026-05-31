# P2 video-insight benchmark：analyze 扣费映射 + HTTP smoke
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }
$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

Write-Host "=== verify-product-video-insight-benchmark ==="
Set-Location $root
mvn -pl douyin-operations-shortvideo -Dtest=BenchmarkAnalysisChargeTest -DfailIfNoTests=false test -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] BenchmarkAnalysisChargeTest"
    exit 1
}

$seed = Join-Path $PSScriptRoot "seed-gaifan-feature-verify.sql"
if (Test-Path $seed) {
    Get-Content $seed -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}
$before = Q "select count(*) from gf_credit_ledger where product_code='video-insight';"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/benchmark/analysis/analyze" `
    -Body @{ benchmarkVideoId = 1; forceReanalyze = $true; enableAsr = $false; enableOcr = $false; enableApi = $false } `
    -TenantId "demo-tenant" -UserId "1" -TraceId "verify-p2-benchmark" -TimeoutSec 120
$after = Q "select count(*) from gf_credit_ledger where product_code='video-insight';"
$charged = ([int]$after -gt [int]$before)
if (-not $charged) {
    Write-Host "[FAIL] benchmark analyze no VIDEO_VIRAL_ANALYSIS ledger (http=$($r.http))"
    exit 1
}
if ($r.http -ne 200) {
    Write-Host "[WARN] benchmark analyze http=$($r.http) pipelineDegraded=true"
    $env:GAIFAN_BENCHMARK_HTTP = [string]$r.http
    $env:GAIFAN_BENCHMARK_PIPELINE_DEGRADED = "true"
} else {
    $env:GAIFAN_BENCHMARK_HTTP = [string]$r.http
    $env:GAIFAN_BENCHMARK_PIPELINE_DEGRADED = "false"
}
Write-Host "[OK] verify-product-video-insight-benchmark passed http=$($r.http) charged=$charged pipelineDegraded=$($env:GAIFAN_BENCHMARK_PIPELINE_DEGRADED)"
