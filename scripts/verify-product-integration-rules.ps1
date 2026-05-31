# Post-M4：互调规则存在性（V216 + V224）
param([string]$PgContainer = "dy-postgres")
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\Invoke-GaifanProductApi.ps1")
function Q($sql) { Invoke-PsqlScalarM4 -Sql $sql -PgContainer $PgContainer }

$rules = @(
    'douyin-to-video-insight',
    'shortvideo-maker-to-digital-human',
    'shortvideo-maker-to-photo-avatar',
    'drama-ai-to-shortvideo-maker'
)
foreach ($code in $rules) {
    $n = Q "select count(*) from gf_product_integration_rule where rule_code='$code' and enabled=true;"
    if ([int]$n -lt 1) {
        Write-Host "[FAIL] missing rule $code"
        exit 1
    }
    Write-Host "[OK] rule $code"
}
Write-Host "[OK] verify-product-integration-rules passed"
