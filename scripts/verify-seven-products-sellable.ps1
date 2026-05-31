# 可售卖总验收：M4 六产品 + KB + JWT E2E
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL,
    [switch]$SkipJwt,
    [switch]$SkipHttp
)

$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }

$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanJwtApi.ps1")

$outDir = Join-Path $root "reports\automation"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$sellablePath = Join-Path $outDir "seven-products-sellable.json"

$doc = @{
    generatedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
    baseUrl       = $BaseUrl
    status        = "running"
    products      = @{}
    m4            = $null
    jwtE2e        = (-not $SkipJwt)
}
$doc | ConvertTo-Json -Depth 8 | Set-Content -Path $sellablePath -Encoding UTF8

Write-Host "=== verify-seven-products-sellable === BaseUrl=$BaseUrl"

pwsh (Join-Path $PSScriptRoot "verify-product-common.ps1")
if ($LASTEXITCODE -ne 0) {
    $doc.status = "failed"
    $doc | ConvertTo-Json -Depth 8 | Set-Content -Path $sellablePath -Encoding UTF8
    exit $LASTEXITCODE
}

if (-not $SkipHttp) {
    pwsh (Join-Path $PSScriptRoot "verify-six-products-m4.ps1") -BaseUrl $BaseUrl -SkipStaging
    if ($LASTEXITCODE -ne 0) {
        $doc.status = "failed"
        $doc | ConvertTo-Json -Depth 8 | Set-Content -Path $sellablePath -Encoding UTF8
        exit $LASTEXITCODE
    }
    $m4Path = Join-Path $outDir "six-products-m4.json"
    if (Test-Path $m4Path) {
        $doc.m4 = Get-Content $m4Path -Raw -Encoding UTF8 | ConvertFrom-Json
        if ($doc.m4.products) {
            $doc.m4.products.PSObject.Properties | ForEach-Object {
                $doc.products[$_.Name] = $_.Value
            }
        }
    }
    pwsh (Join-Path $PSScriptRoot "verify-product-knowledge-base.ps1") -BaseUrl $BaseUrl
    if ($LASTEXITCODE -ne 0) {
        $doc.status = "failed"
        $doc | ConvertTo-Json -Depth 8 | Set-Content -Path $sellablePath -Encoding UTF8
        exit $LASTEXITCODE
    }
}

if (-not $SkipJwt) {
    $env:GAIFAN_VERIFY_BASE_URL = $BaseUrl
    pwsh (Join-Path $PSScriptRoot "verify-product-jwt-e2e.ps1") -BaseUrl $BaseUrl
    if ($LASTEXITCODE -ne 0) {
        $doc.status = "failed"
        if (Test-Path $sellablePath) {
            try {
                $existing = Get-Content $sellablePath -Raw -Encoding UTF8 | ConvertFrom-Json
                if ($existing.products) {
                    $existing.products.PSObject.Properties | ForEach-Object { $doc.products[$_.Name] = $_.Value }
                }
            } catch { }
        }
        $doc | ConvertTo-Json -Depth 8 | Set-Content -Path $sellablePath -Encoding UTF8
        exit $LASTEXITCODE
    }
}

if (Test-Path $sellablePath) {
    try {
        $existing = Get-Content $sellablePath -Raw -Encoding UTF8 | ConvertFrom-Json
        if ($existing.products) {
            $existing.products.PSObject.Properties | ForEach-Object { $doc.products[$_.Name] = $_.Value }
        }
    } catch { }
}

$doc.status = "passed"
$doc.completedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
$doc | ConvertTo-Json -Depth 10 | Set-Content -Path $sellablePath -Encoding UTF8
Write-Host "[OK] verify-seven-products-sellable passed -> $sellablePath"
