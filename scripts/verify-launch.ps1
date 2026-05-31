# dy05 Gaifan 升级后启动验证（M1 + Wave4 + DB 断言）
param(
    [string]$BaseUrl = "http://localhost:18081",
    [switch]$AssertDb,
    [switch]$SkipPayment,
    [switch]$RunPaymentTest,
    [string]$DbHost = "localhost",
    [int]$DbPort = 5433,
    [string]$DbName = "douyin_operations",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "postgresql",
    [string]$PgContainer = "dy-postgres",
    [long]$ViralVideoId = 1
)

$ErrorActionPreference = "Stop"
$script:TracePrefix = "verify-launch"

function Invoke-Check {
    param(
        [string]$Name,
        [string]$Method = "GET",
        [string]$Url,
        [string]$Body = $null,
        [int[]]$ExpectedStatus = @(200)
    )
    try {
        if ($Method -eq "POST" -and $Body) {
            $r = Invoke-WebRequest -Uri $Url -Method POST -Body $Body -ContentType "application/json" -UseBasicParsing -TimeoutSec 60
        } else {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 60
        }
        if ($ExpectedStatus -notcontains $r.StatusCode) {
            Write-Host "[FAIL] $Name expected $($ExpectedStatus -join '|') got $($r.StatusCode)"
            return $false
        }
        Write-Host "[OK] $Name $($r.StatusCode)"
        return $true
    } catch {
        $code = $null
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        if ($code -and ($ExpectedStatus -contains $code)) {
            Write-Host "[OK] $Name $code (expected)"
            return $true
        }
        Write-Host "[FAIL] $Name $_"
        return $false
    }
}

function Invoke-PsqlScalar {
    param([string]$Sql)
    $env:PGPASSWORD = $DbPassword
    try {
        $out = docker exec -e PGPASSWORD=$DbPassword $PgContainer psql -v ON_ERROR_STOP=1 -U $DbUser -d $DbName -t -A -c $Sql 2>&1
        if ($LASTEXITCODE -ne 0) { throw "docker psql failed: $out" }
        return ($out | Out-String).Trim()
    } finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
}

function Assert-Database {
    Write-Host "--- DB assertions ---"
    $demoAccount = Invoke-PsqlScalar "select count(*) from gf_credit_account where tenant_id='demo-tenant';"
    if ([int]$demoAccount -lt 1) {
        Write-Host "[FAIL] gf_credit_account demo-tenant missing"
        return $false
    }
    Write-Host "[OK] gf_credit_account demo-tenant exists"

    $ledgerTrace = Invoke-PsqlScalar "select count(*) from gf_credit_ledger where reason like '%${script:TracePrefix}%';"
    if ([int]$ledgerTrace -lt 1) {
        Write-Host "[FAIL] gf_credit_ledger missing reason %${script:TracePrefix}%"
        return $false
    }
    Write-Host "[OK] gf_credit_ledger reason ${script:TracePrefix} count=$ledgerTrace"

    $mcpInv = Invoke-PsqlScalar "select count(*) from gf_mcp_invocation where tool_code='video.analyze' and trace_id='${script:TracePrefix}-wave4';"
    if ([int]$mcpInv -lt 1) {
        Write-Host "[WARN] gf_mcp_invocation video.analyze not found (seed viral video?)"
    } else {
        Write-Host "[OK] gf_mcp_invocation video.analyze count=$mcpInv"
    }

    $aiTraceExact = "$($script:TracePrefix)-ai-call"
    $aiInv = Invoke-PsqlScalar "select count(*) from gf_ai_invocation where trace_id='$aiTraceExact';"
    if ([int]$aiInv -lt 1) {
        Write-Host "[FAIL] gf_ai_invocation missing trace_id=$aiTraceExact (POST /api/v1/ai/call + X-Tenant-Id?)"
        return $false
    }
    Write-Host "[OK] gf_ai_invocation trace_id=$aiTraceExact count=$aiInv"
    return $true
}

$checks = @(
    @{ Name = "health"; Url = "$BaseUrl/actuator/health" },
    @{ Name = "credits-account"; Url = "$BaseUrl/api/credits/account?tenantId=demo-tenant" },
    @{ Name = "credits-overview"; Url = "$BaseUrl/api/credits/overview?tenantId=demo-tenant" },
    @{ Name = "public-site"; Url = "$BaseUrl/api/public-site/overview" },
    @{ Name = "mcp-tools-v1"; Url = "$BaseUrl/api/v1/mcp/tools" },
    @{ Name = "mcp-overview"; Url = "$BaseUrl/api/mcp/overview" },
    @{ Name = "mcp-runtime"; Url = "$BaseUrl/api/mcp/runtime" },
    @{ Name = "openapi-overview"; Url = "$BaseUrl/api/openapi/overview?tenantId=demo-tenant" }
)

foreach ($c in $checks) {
    if (-not (Invoke-Check -Name $c.Name -Url $c.Url)) { exit 1 }
}

$consumeBody = @{
    tenantId = "demo-tenant"
    userId = "demo-user"
    productCode = "video-insight"
    featureCode = "video-insight.breakdown"
    channel = "WEB"
    requestedAmount = 1
    traceId = "$($script:TracePrefix)-consume"
    reason = "verify-launch.ps1"
} | ConvertTo-Json -Compress

if (-not (Invoke-Check -Name "credits-consume" -Method "POST" -Url "$BaseUrl/api/credits/consume" -Body $consumeBody)) {
    exit 1
}

$rpcList = '{"jsonrpc":"2.0","id":"verify","method":"tools/list"}'
if (-not (Invoke-Check -Name "mcp-jsonrpc-tools-list" -Method "POST" -Url "$BaseUrl/mcp" -Body $rpcList)) {
    exit 1
}

$wave4Trace = "$($script:TracePrefix)-wave4"
$rpcCall = (@{
    jsonrpc = "2.0"
    id = "verify-wave4"
    method = "tools/call"
    params = @{
        name = "video.analyze"
        tenantId = "demo-tenant"
        userId = "mcp-user"
        traceId = $wave4Trace
        arguments = @{
            viralVideoId = [string]$ViralVideoId
            userId = "1"
        }
    }
} | ConvertTo-Json -Compress -Depth 6)

if (-not (Invoke-Check -Name "mcp-jsonrpc-video-analyze" -Method "POST" -Url "$BaseUrl/mcp" -Body $rpcCall)) {
    exit 1
}

$aiTrace = "$($script:TracePrefix)-ai-call"
$aiCallBody = (@{
    featureCode = "ai.chat"
    prompt = "verify-launch smoke"
} | ConvertTo-Json -Compress)
$aiHeaders = @{
    "X-Trace-Id" = $aiTrace
    "X-Tenant-Id" = "demo-tenant"
    "X-User-Id" = "demo-user"
}
try {
    $aiResp = Invoke-WebRequest -Uri "$BaseUrl/api/v1/ai/call" -Method POST -Body $aiCallBody `
        -ContentType "application/json" -Headers $aiHeaders -UseBasicParsing -TimeoutSec 60
    if ($aiResp.StatusCode -eq 200 -or $aiResp.StatusCode -eq 402) {
        Write-Host "[OK] ai-call $($aiResp.StatusCode)"
    } else {
        Write-Host "[FAIL] ai-call unexpected $($aiResp.StatusCode)"
        exit 1
    }
} catch {
    $code = $null
    if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
    if ($code -eq 200 -or $code -eq 402) {
        Write-Host "[OK] ai-call $code"
    } else {
        Write-Host "[FAIL] ai-call $_"
        exit 1
    }
}

if (-not $SkipPayment) {
    if ($RunPaymentTest) {
        Write-Host "--- PaymentCreditGrantAdapterPgTest ---"
        Push-Location (Split-Path $PSScriptRoot -Parent)
        mvn -pl douyin-operations-app test "-Dtest=PaymentCreditGrantAdapterPgTest" -q
        if ($LASTEXITCODE -ne 0) { Pop-Location; exit 1 }
        Pop-Location
        Write-Host "[OK] PaymentCreditGrantAdapterPgTest"
    } else {
        Write-Host "[SKIP] payment: use -RunPaymentTest for PaymentCreditGrantAdapterPgTest"
    }
}

if ($AssertDb) {
    if (-not (Assert-Database)) { exit 1 }
}

Write-Host "verify-launch passed (consume + MCP tools/call + optional DB/payment)"
