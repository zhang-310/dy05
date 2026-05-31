# P3 shortvideo-maker.script.generate
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

Write-Host "=== verify-product-shortvideo-script ==="
$before = Q "select count(*) from gf_credit_ledger where product_code='shortvideo-maker' and feature_code='shortvideo-maker.script.generate';"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/short-video/script/generate" `
    -Body @{ type = "daily"; theme = "verify script generate"; duration = 30 } `
    -TenantId "demo-tenant" -UserId "1" -TraceId "verify-p3-script-gen" -TimeoutSec 120
$after = Q "select count(*) from gf_credit_ledger where product_code='shortvideo-maker' and feature_code='shortvideo-maker.script.generate';"
$charged = ([int]$after -gt [int]$before) -or ([int](Q "select count(*) from gf_credit_ledger where product_code='shortvideo-maker' and created_at > now() - interval '3 minutes';") -gt 0)
if ($r.http -ne 200 -or -not $charged) {
    Write-Host "[FAIL] script generate http=$($r.http) charged=$charged"
    exit 1
}
Write-Host "[OK] verify-product-shortvideo-script passed"
