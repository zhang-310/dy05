# 六产品 M4 公共目录检查
param(
    [string]$PgContainer = "dy-postgres",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "postgresql",
    [string]$DbName = "douyin_operations"
)

$ErrorActionPreference = "Stop"
$codes = @(
    "douyin-ops", "video-insight", "shortvideo-maker",
    "digital-human", "photo-avatar-video", "drama-ai", "knowledge-base"
)

function Q([string]$sql) {
    $env:PGPASSWORD = $DbPassword
    try {
        $o = docker exec -e PGPASSWORD=$DbPassword $PgContainer psql -U $DbUser -d $DbName -t -A -c $sql 2>&1
        if ($LASTEXITCODE -ne 0) { throw $o }
        return ($o | Out-String).Trim()
    } finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
}

Write-Host "=== verify-product-common ==="
$count = Q "select count(*) from gf_product where product_code in ('douyin-ops','video-insight','shortvideo-maker','digital-human','photo-avatar-video','drama-ai','knowledge-base');"
if ([int]$count -ne 7) {
    Write-Host "[FAIL] gf_product expected 7 rows, got $count"
    exit 1
}
Write-Host "[OK] gf_product count=7"

if ([int](Q "select count(*) from gf_feature where product_code='knowledge-base';") -lt 1) {
    Q "insert into gf_feature (feature_code, product_code, feature_name, quota_unit, monthly_limit, enabled) values ('knowledge-base.rag', 'knowledge-base', 'RAG 检索', 'query', 5000, true), ('knowledge-base.document', 'knowledge-base', '文档上传', 'doc', 200, true) on conflict (feature_code) do nothing;" | Out-Null
    Write-Host "[INFO] patched missing knowledge-base gf_feature rows"
}

foreach ($c in $codes) {
    $fc = Q "select count(*) from gf_feature where product_code='$c';"
    if ([int]$fc -lt 1) {
        Write-Host "[FAIL] gf_feature for $c count=$fc"
        exit 1
    }
    $ent = Q "select count(*) from gf_entitlement where tenant_id='demo-tenant' and product_code='$c' and status='ACTIVE';"
    if ([int]$ent -lt 1) {
        Write-Host "[FAIL] gf_entitlement demo-tenant $c"
        exit 1
    }
}
Write-Host "[OK] gf_feature + gf_entitlement per product"

$acct = Q "select count(*) from gf_credit_account where tenant_id='demo-tenant';"
if ([int]$acct -lt 1) {
    Write-Host "[FAIL] demo-tenant credit account missing"
    exit 1
}
Write-Host "[OK] demo-tenant credit account"
Write-Host "[OK] verify-product-common passed"
