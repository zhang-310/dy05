# ai.mcp-tool：McpInvokeService 积分/权益单测门禁
param()
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
Write-Host "=== verify-product-mcp-credit ==="
mvn -q -pl douyin-operations-app test "-Dtest=McpInvokeServiceCreditTest" -DfailIfNoTests=false
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] McpInvokeServiceCreditTest"
    exit 1
}
Write-Host "[OK] verify-product-mcp-credit passed"
