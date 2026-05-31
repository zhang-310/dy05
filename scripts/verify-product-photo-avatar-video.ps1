# P5 photo-avatar-video M4 verify
param(
    [string]$BaseUrl = "http://localhost:18081",
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

$seed = Join-Path $PSScriptRoot "seed-gaifan-feature-verify.sql"
if (Test-Path $seed) {
    Get-Content $seed -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
}

try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up"; exit 1 }

$trace = "verify-p5-photo-avatar"
$insightReportId = 1
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/photo-avatar/create" `
    -Body @{
        photoUrl = "https://example.com/photo.jpg"
        outfitStyle = "casual"
        background = "studio"
        portraitConsentConfirmed = $true
        insightReportId = $insightReportId
    } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $trace
$ledger = Q "select count(*) from gf_credit_ledger where product_code='photo-avatar-video' and (created_at > now() - interval '2 minutes' or trace_id='$trace');"
$delivery = Q "select count(*) from gf_photo_avatar_video_delivery_workspace_ledger where trace_id='$trace' or created_at > now() - interval '2 minutes';"
$ok = ($r.http -eq 200) -and ([int]$ledger -gt 0) -and ([int]$delivery -gt 0)
$linked = Q "select count(*) from gf_credit_ledger where product_code='photo-avatar-video' and reason like '%insightReportId=1%' and created_at > now() - interval '5 minutes';"
if (-not $ok) {
    Write-Host "[FAIL] photo-avatar http=$($r.http) ledger=$ledger delivery=$delivery"
    Update-SixProductsM4Json -ProductCode "photo-avatar-video" -Status "failed" -Checks @{ http = $r.http }
    exit 1
}
Write-Host "[OK] photo-avatar create"
Update-SixProductsM4Json -ProductCode "photo-avatar-video" -Checks @{ http = $r.http; ledger = $ledger; delivery = $delivery; insightReportId = $insightReportId; linked = $linked }
Write-Host "[OK] verify-product-photo-avatar-video passed"
