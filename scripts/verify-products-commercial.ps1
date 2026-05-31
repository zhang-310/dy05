# 六产品商业化抽样 HTTP 冒烟（需 gaifan-dev 已启动且 demo-tenant 有积分）
param(
    [string]$BaseUrl = "http://localhost:18081",
    [string]$TenantId = "demo-tenant",
    [switch]$SkipAuth
)

$ErrorActionPreference = "Stop"
$results = [ordered]@{}

function Test-Endpoint($name, $method, $path, $body) {
    try {
        $uri = "$BaseUrl$path"
        $params = @{ Uri = $uri; Method = $method; TimeoutSec = 30 }
        if ($body) {
            $params.ContentType = "application/json"
            $params.Body = ($body | ConvertTo-Json -Depth 6)
        }
        $resp = Invoke-WebRequest @params
        $results[$name] = @{ ok = $true; status = $resp.StatusCode }
        Write-Host "[OK] $name -> $($resp.StatusCode)"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        $results[$name] = @{ ok = ($code -eq 402); status = $code; note = "enforce may return 402" }
        Write-Host "[INFO] $name -> $code (402=expected when enforce+no credit)"
    }
}

Write-Host "[1] public-site overview"
Test-Endpoint "public-overview" "GET" "/api/public-site/overview" $null

Write-Host "[2] MCP tools list (no invoke without token)"
Test-Endpoint "mcp-tools" "GET" "/api/mcp/tools" $null

$outDir = Join-Path (Split-Path $PSScriptRoot -Parent) "reports\automation"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$outFile = Join-Path $outDir "verify-products-commercial.json"
$results | ConvertTo-Json -Depth 4 | Set-Content -Path $outFile -Encoding UTF8
Write-Host "[DONE] wrote $outFile"
