# Post-M4：互调 E2E — 4 条规则 HTTP + DB 台账
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$PgContainer = "dy-postgres"
)
$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")
. (Join-Path $PSScriptRoot "lib\New-GaifanDailyProjectId.ps1")

function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

function Invoke-IntegrationGate {
    param($SourceProduct, $SourceFeature, $TargetProduct, $TargetFeature, $TraceId)
    Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/platform/product-integrations/invoke" `
        -Body @{
            tenantId = "demo-tenant"
            userId = "1"
            sourceProductCode = $SourceProduct
            sourceFeatureCode = $SourceFeature
            targetProductCode = $TargetProduct
            targetFeatureCode = $TargetFeature
            channel = "WEB"
            requestedAmount = 1
            traceId = $TraceId
            invocationInputSummary = "verify integration $TraceId"
            createWorkflowTask = $false
            reviewRequired = $false
        } -TenantId "demo-tenant" -UserId "1" -TraceId $TraceId
}

Write-Host "=== verify-product-integration-e2e === BaseUrl=$BaseUrl"
$seed = Join-Path $PSScriptRoot "seed-gaifan-feature-verify.sql"
$p2 = Join-Path $PSScriptRoot "seed-gaifan-p2-viral-demo.sql"
foreach ($s in @($seed, $p2)) {
    if (Test-Path $s) {
        Get-Content $s -Raw | docker exec -i -e PGPASSWORD=postgresql $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
    }
}

pwsh (Join-Path $PSScriptRoot "verify-product-integration-rules.ps1") -PgContainer $PgContainer
if ($LASTEXITCODE -ne 0) { exit 1 }

try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up"; exit 1 }

# 1) drama-ai → shortvideo-maker
$exportTrace = "verify-integration-drama-export-$(Get-Date -Format 'yyyyMMddHHmmss')"
$create = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/drama/create" `
    -Body @{ title = "integration e2e drama"; genre = "comedy"; episodeCount = 1 } `
    -TenantId "demo-tenant" -UserId "1" -TraceId "verify-integration-drama-create"
if ($create.http -ne 200) { Write-Host "[FAIL] drama create"; exit 1 }
$projectId = Q "select id from drama_project where deleted=0 order by id desc limit 1;"
$re = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/drama/export-to-maker" `
    -Body @{ id = [int]$projectId; traceId = $exportTrace } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $exportTrace
$inv1 = Q "select count(*) from gf_product_integration_invocation where trace_id='$exportTrace' and source_product_code='drama-ai';"
$del1 = Q "select count(*) from gf_shortvideo_maker_delivery_workspace_ledger where trace_id='$exportTrace';"
if (-not (($re.http -eq 200) -and ([int]$inv1 -gt 0) -and ([int]$del1 -gt 0))) {
    Write-Host "[FAIL] drama→maker http=$($re.http) inv=$inv1 del=$del1"; exit 1
}
Write-Host "[OK] drama-ai-to-shortvideo-maker inv=$inv1 del=$del1"

# 2) douyin-to-video-insight
$trace2 = "verify-integration-douyin-insight-$(Get-Date -Format 'HHmmss')"
$g2 = Invoke-IntegrationGate -SourceProduct "douyin-ops" -SourceFeature "douyin-ops.content-planning" `
    -TargetProduct "video-insight" -TargetFeature "video.analyze.standard" -TraceId $trace2
$inv2 = Q "select count(*) from gf_product_integration_invocation where trace_id='$trace2' and source_product_code='douyin-ops' and target_product_code='video-insight';"
$da = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/short-video/viral/deep-analyze" `
    -Body @{ id = 1 } -TenantId "demo-tenant" -UserId "1" -TraceId "$trace2-deep" -TimeoutSec 90
$del2 = Q "select count(*) from gf_video_insight_delivery_workspace_ledger where created_at > now() - interval '5 minutes';"
if ([int]$inv2 -lt 1 -or $da.http -ne 200) {
    Write-Host "[FAIL] douyin→insight inv=$inv2 deep=$($da.http)"; exit 1
}
Write-Host "[OK] douyin-to-video-insight inv=$inv2 deep=$($da.http) delivery_recent=$del2"

# 3) shortvideo-maker-to-digital-human
$trace3 = "verify-integration-maker-dh-$(Get-Date -Format 'HHmmss')"
$makerProject = New-GaifanDailyProjectId -BaseUrl $BaseUrl -TraceId "verify-int-dh-proj"
$g3 = Invoke-IntegrationGate -SourceProduct "shortvideo-maker" -SourceFeature "shortvideo-maker.export" `
    -TargetProduct "digital-human" -TargetFeature "digital-human.video.synthesize" -TraceId $trace3
$inv3 = Q "select count(*) from gf_product_integration_invocation where trace_id='$trace3';"
$dh = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/digital-human/create" `
    -Body @{ scriptContent = "integration dh"; voiceType = "default" } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $trace3
$del3 = Q "select count(*) from gf_digital_human_delivery_workspace_ledger where trace_id='$trace3' or created_at > now() - interval '5 minutes';"
$wf = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/short-video/workflow/digital-human-commerce/start" `
    -Body @{ projectId = $makerProject } -TenantId "demo-tenant" -UserId "1" -TraceId "$trace3-wf"
if ([int]$inv3 -lt 1 -or $dh.http -ne 200 -or $wf.http -ne 200) {
    Write-Host "[FAIL] maker→dh inv=$inv3 dh=$($dh.http) wf=$($wf.http)"; exit 1
}
Write-Host "[OK] shortvideo-maker-to-digital-human inv=$inv3 dh=$($dh.http) wf=$($wf.http) del=$del3"

# 4) shortvideo-maker-to-photo-avatar
$trace4 = "verify-integration-maker-photo-$(Get-Date -Format 'HHmmss')"
$g4 = Invoke-IntegrationGate -SourceProduct "shortvideo-maker" -SourceFeature "shortvideo-maker.export" `
    -TargetProduct "photo-avatar-video" -TargetFeature "photo-avatar.video.synthesize" -TraceId $trace4
$inv4 = Q "select count(*) from gf_product_integration_invocation where trace_id='$trace4';"
$photo = Invoke-GaifanProductApi -BaseUrl $BaseUrl -Path "/api/v1/photo-avatar/create" `
    -Body @{ photoUrl = "https://example.com/p.jpg"; portraitConsentConfirmed = $true; insightReportId = 1; scriptText = "hi" } `
    -TenantId "demo-tenant" -UserId "1" -TraceId $trace4
$del4 = Q "select count(*) from gf_photo_avatar_video_delivery_workspace_ledger where trace_id='$trace4' or created_at > now() - interval '5 minutes';"
if ([int]$inv4 -lt 1 -or $photo.http -ne 200) {
    Write-Host "[FAIL] maker→photo inv=$inv4 photo=$($photo.http)"; exit 1
}
Write-Host "[OK] shortvideo-maker-to-photo-avatar inv=$inv4 photo=$($photo.http) del=$del4"

Write-Host "[OK] verify-product-integration-e2e passed (4 rules)"
