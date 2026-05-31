# dy05 独立 Gaifan prod 微服务栈（8088，不依赖 gaifan-ops）
param(
    [switch]$SkipBuild,
    [switch]$SkipFlyway,
    [string]$PgContainer = "dy-postgres"
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

Write-Host "=== dy05 Gaifan prod stack (8088) ==="

if (-not $SkipFlyway) {
    pwsh scripts/apply-gaifan-product-flyway.ps1 -PgContainer $PgContainer
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if (-not $SkipBuild) {
    Write-Host "[BUILD] backend..."
    mvn -pl douyin-operations-app -am install -DskipTests -q
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Write-Host "[BUILD] frontend..."
    Push-Location front
    npm run build --silent
    if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
    Pop-Location

    Write-Host "[BUILD] docker images..."
    docker build -t dy05-backend:latest -f docker/Dockerfile.backend .
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    docker build -t dy05-frontend:latest -f docker/frontend.Dockerfile .
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "[UP] docker compose microservices..."
docker compose -f docker-compose.microservices.yml up -d --build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[WAIT] gateway health..."
$ok = $false
for ($i = 0; $i - 30; $i++) {
    try {
        $h = Invoke-WebRequest -Uri "http://localhost:8088/health" -UseBasicParsing -TimeoutSec 3
        if ($h.StatusCode -eq 200) { $ok = $true; break }
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $ok) {
    Write-Host "[FAIL] gateway /health not ready"
    docker compose -f docker-compose.microservices.yml ps
    exit 1
}

Write-Host "[PROBE] POST douyin-ops brief (header identity for smoke bootstrap)..."
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8088/api/v1/ai/douyin-ops-commander/brief" `
        -Method POST -Headers @{ "X-Tenant-Id" = "demo-tenant"; "X-User-Id" = "1" } `
        -ContentType "application/json" -Body "{}" -UseBasicParsing -TimeoutSec 120
    Write-Host "[OK] brief http=$($r.StatusCode)"
} catch {
    $code = 0
    if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
    Write-Host "[WARN] brief probe http=$code (JWT-only prod may need token; routing should not be 405)"
    if ($code -eq 405) { exit 1 }
}

Write-Host "[OK] start-gaifan-prod-stack completed -> http://localhost:8088"
