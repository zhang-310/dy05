# Feature §5 全量验收矩阵
param(
    [string]$BaseUrl = $env:GAIFAN_VERIFY_BASE_URL
)

$ErrorActionPreference = "Stop"
if (-not $BaseUrl) { $BaseUrl = "http://localhost:18082" }

$root = Split-Path $PSScriptRoot -Parent
$outDir = Join-Path $root "reports\automation"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$matrixPath = Join-Path $outDir "seven-products-feature-matrix.json"

$matrix = @{
    generatedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
    baseUrl     = $BaseUrl
    status      = "running"
    features    = @{}
}

function Run-FeatureStep([string]$Key, [string]$Script, [string[]]$ExtraArgs = @()) {
    Write-Host "--- feature: $Key ---"
    $args = @("-BaseUrl", $BaseUrl) + $ExtraArgs
    pwsh (Join-Path $PSScriptRoot $Script) @args
    if ($LASTEXITCODE -ne 0) {
        $matrix.features[$Key] = @{ status = "failed"; script = $Script }
        $matrix.status = "failed"
        $matrix | ConvertTo-Json -Depth 8 | Set-Content -Path $matrixPath -Encoding UTF8
        exit 1
    }
    $matrix.features[$Key] = @{ status = "passed"; script = $Script; completedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ") }
}

Write-Host "=== verify-product-feature-matrix === BaseUrl=$BaseUrl"

if ($env:GAIFAN_MATRIX_SKIP_M4 -ne "true") {
    Run-FeatureStep "m4-six-products" "verify-six-products-m4.ps1" @("-SkipStaging")
}
Run-FeatureStep "knowledge-base.rag" "verify-product-knowledge-base.ps1"
Run-FeatureStep "knowledge-base.document" "verify-product-knowledge-base-document.ps1"
Run-FeatureStep "douyin-ops.video-analysis" "verify-product-douyin-ops-video-analysis.ps1"
Run-FeatureStep "douyin-ops.oauth" "verify-product-douyin-ops-oauth.ps1"
Run-FeatureStep "douyin-ops.payment-http" "verify-product-douyin-ops-payment-http.ps1"
Run-FeatureStep "douyin-ops.payment-callback" "verify-product-douyin-ops-payment-callback.ps1"
Run-FeatureStep "video-insight.benchmark" "verify-product-video-insight-benchmark.ps1"
if ($env:GAIFAN_BENCHMARK_HTTP) {
    $matrix.features["video-insight.benchmark"].httpStatus = [int]$env:GAIFAN_BENCHMARK_HTTP
    if ($env:GAIFAN_BENCHMARK_PIPELINE_DEGRADED -eq "true") {
        $matrix.features["video-insight.benchmark"].pipelineDegraded = $true
    }
}
Run-FeatureStep "shortvideo-maker.script" "verify-product-shortvideo-script.ps1"
Run-FeatureStep "shortvideo-maker.workflow" "verify-product-shortvideo-workflow.ps1"
Run-FeatureStep "drama-ai.script" "verify-product-drama-script.ps1"
Run-FeatureStep "ai.mcp-tool" "verify-product-mcp-credit.ps1"
Run-FeatureStep "integration-e2e-4rules" "verify-product-integration-e2e.ps1"

pwsh (Join-Path $PSScriptRoot "verify-product-douyin-ops-payment.ps1")
if ($LASTEXITCODE -ne 0) {
    $matrix.features["douyin-ops.payment-pg"] = @{ status = "failed" }
    $matrix.status = "failed"
    $matrix | ConvertTo-Json -Depth 8 | Set-Content -Path $matrixPath -Encoding UTF8
    exit 1
}
$matrix.features["douyin-ops.payment-pg"] = @{ status = "passed" }

$matrix.status = "passed"
$matrix.completedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
$matrix | ConvertTo-Json -Depth 10 | Set-Content -Path $matrixPath -Encoding UTF8
Write-Host "[OK] verify-product-feature-matrix passed -> $matrixPath"
