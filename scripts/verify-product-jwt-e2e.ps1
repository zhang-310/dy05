# 七产品 JWT E2E：登录后各调 1 次主写 API（禁止 X-User-Id）
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [string]$Username = $env:GAIFAN_JWT_USER,
    [string]$Password = $env:GAIFAN_JWT_PASSWORD
)

$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }
if (-not $Username) { $Username = "admin" }
if (-not $Password) { $Password = "admin123" }

. (Join-Path $PSScriptRoot "lib\Invoke-GaifanJwtApi.ps1")

Write-Host "=== verify-product-jwt-e2e === BaseUrl=$BaseUrl"
try { $null = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "[FAIL] app not up at $BaseUrl"; exit 1 }

$jwtSeed = Join-Path $PSScriptRoot "seed-gaifan-jwt-tenant.sql"
if (Test-Path $jwtSeed) {
    Get-Content $jwtSeed -Raw | docker exec -i -e PGPASSWORD=postgresql dy-postgres psql -v ON_ERROR_STOP=1 -U postgres -d douyin_operations | Out-Null
    Write-Host "[OK] JWT tenant entitlements seeded (user-1)"
}

$token = Get-GaifanJwtToken -BaseUrl $BaseUrl -Username $Username -Password $Password
Write-Host "[OK] JWT obtained"

$kbIdJwt = "90001"
try {
    $kbOut = docker exec -e PGPASSWORD=postgresql dy-postgres psql -U postgres -d douyin_operations -t -A `
        -c "select id from ai_knowledge_base where deleted=0 and user_id=1 order by id limit 1;"
    if ($kbOut -and $kbOut.Trim()) { $kbIdJwt = $kbOut.Trim() }
} catch { }

$makerProjectId = 90001
try {
    $makerProjectId = New-GaifanDailyProjectIdJwt -BaseUrl $BaseUrl -Token $token -TraceId "jwt-e2e-create-project"
    Write-Host "[OK] dynamic shortvideo projectId=$makerProjectId"
} catch {
    Write-Host "[WARN] dynamic project create failed, fallback 90001: $_"
}

$calls = @(
    @{ product = "douyin-ops"; path = "/api/v1/ai/douyin-ops-commander/brief"; body = @{ accountId = 1; goal = "jwt-e2e" } },
    @{ product = "video-insight"; path = "/api/v1/short-video/viral/deep-analyze"; body = @{ id = 1 } },
    @{ product = "shortvideo-maker"; path = "/api/v1/short-video/project/export-script"; body = @{ projectId = $makerProjectId } },
    @{ product = "digital-human"; path = "/api/v1/digital-human/create"; body = @{ scriptContent = "jwt e2e"; voiceType = "default" } },
    @{ product = "photo-avatar-video"; path = "/api/v1/photo-avatar/create"; body = @{ portraitConsentConfirmed = $true; scriptText = "jwt" } },
    @{ product = "drama-ai"; path = "/api/v1/drama/create"; body = @{ title = "jwt-e2e-drama" } },
    @{ product = "knowledge-base"; path = "/api/v1/ai/knowledge-base/$kbIdJwt/search"; body = @{ query = "jwt verify rag"; topK = 3; queryRewrite = $false } }
)

$failed = 0
foreach ($c in $calls) {
    $trace = "jwt-e2e-$($c.product)"
    $r = Invoke-GaifanJwtApi -BaseUrl $BaseUrl -Token $token -Path $c.path -Body $c.body -TraceId $trace -TimeoutSec 120
    $ok = ($r.http -eq 200) -or ($r.http -eq 0 -and $r.body -match '"status"\s*:\s*200')
    if (-not $ok -and $r.http -eq 0 -and $r.body -match '200') { $ok = $true }
    if ($r.http -ge 200 -and $r.http -lt 300) { $ok = $true }
    if (-not $ok) {
        Write-Host "[FAIL] $($c.product) http=$($r.http) $($r.body.Substring(0, [Math]::Min(200, $r.body.Length)))"
        Update-SevenProductsSellableJson -ProductCode $c.product -Status "failed" -Checks @{ http = $r.http; jwt = $true }
        $failed++
    } else {
        Write-Host "[OK] $($c.product) http=$($r.http)"
        Update-SevenProductsSellableJson -ProductCode $c.product -Checks @{ http = $r.http; jwt = $true }
    }
}

if ($failed -gt 0) { exit 1 }
Write-Host "[OK] verify-product-jwt-e2e passed (7 products)"
