# P4 digital-human M4 verify
param(
    [string]$BaseUrl = "http://localhost:18081",
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up"; exit 1 }

$trace = "verify-p4-digital-human"
$r = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/digital-human/create" `
    -Body @{ scriptContent = "M4 verify script"; voiceType = "default" } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $trace
$ledger = Q "select count(*) from gf_credit_ledger where product_code='digital-human' and created_at > now() - interval '2 minutes';"
$delivery = Q "select count(*) from gf_digital_human_delivery_workspace_ledger where trace_id='$trace';"
$ok = ($r.http -eq 200) -and ([int]$ledger -gt 0) -and ([int]$delivery -gt 0)
if (-not $ok) {
    Write-Host "[FAIL] digital-human http=$($r.http) ledger=$ledger delivery=$delivery"
    Update-SixProductsM4Json -ProductCode "digital-human" -Status "failed" -Checks @{ http = $r.http }
    exit 1
}
Write-Host "[OK] digital-human create"
Update-SixProductsM4Json -ProductCode "digital-human" -Checks @{ http = $r.http; ledger = $ledger; delivery = $delivery }

# Webhook 模拟：HeyGen 完成回调写 delivery
$whTrace = "verify-p4-dh-webhook-$(Get-Date -Format 'HHmmss')"
docker exec -e PGPASSWORD=postgresql $PgContainer psql -U postgres -d douyin_operations -v ON_ERROR_STOP=1 -c @"
INSERT INTO sv_digital_human_task (user_id, provider, external_task_id, status, deleted, create_time)
SELECT 1, 'heygen', '$whTrace', 'SUBMITTED', 0, current_timestamp
WHERE NOT EXISTS (SELECT 1 FROM sv_digital_human_task WHERE external_task_id = '$whTrace' AND deleted = 0);
"@ | Out-Null
$wh = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Method POST -Path "/api/v1/short-video/webhooks/heygen" `
    -Body @{
        event_type = "avatar_video.success"
        data = @{ video_id = $whTrace; video_url = "https://example.com/v.mp4" }
    } -TenantId "demo-tenant" -UserId "1" -TraceId $whTrace
$whDelivery = Q "select count(*) from gf_digital_human_delivery_workspace_ledger where status='WEBHOOK_COMPLETED' and created_at > now() - interval '2 minutes';"
if ($wh.http -ne 200 -or [int]$whDelivery -lt 1) {
    Write-Host "[FAIL] heygen webhook http=$($wh.http) webhook_delivery=$whDelivery"
    exit 1
}
Write-Host "[OK] heygen webhook delivery=$whDelivery"
Write-Host "[OK] verify-product-digital-human passed"
