param(
    [string]$BaseUrl = "http://localhost:8888",
    [string]$Token = $env:DY_AUTH_TOKEN
)

$ErrorActionPreference = "Stop"

function Invoke-DyPost {
    param(
        [string]$Path,
        [hashtable]$Body
    )
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    $json = $Body | ConvertTo-Json -Depth 10
    Invoke-RestMethod -Method Post -Uri "$BaseUrl$Path" -Headers $headers -Body $json
}

function Assert-Field {
    param(
        [object]$Object,
        [string]$Name,
        [string]$Path
    )
    if ($null -eq $Object.$Name) {
        throw "$Path 缺少字段: $Name"
    }
}

Write-Host "验证官方规则引用闭环: $BaseUrl"
if (-not $Token) {
    Write-Host "未设置 DY_AUTH_TOKEN；如接口需要登录，本脚本会在 401/403 处失败。"
}

$violationPath = "/api/v1/short-video/ai/check-violation-rich"
$violation = Invoke-DyPost -Path $violationPath -Body @{
    content = "这是一段测试话术，包含绝对化承诺：三天必白、永久祛斑。"
    scene = "short_video"
}
Assert-Field -Object $violation -Name "officialReferenceRequired" -Path $violationPath
Assert-Field -Object $violation -Name "officialReferenceSatisfied" -Path $violationPath
Assert-Field -Object $violation -Name "officialReferenceStatus" -Path $violationPath
Write-Host "短视频违规审核字段 OK: required=$($violation.officialReferenceRequired), satisfied=$($violation.officialReferenceSatisfied), status=$($violation.officialReferenceStatus)"

if ($env:DY_VERIFY_PROJECT_ID) {
    $reviewPath = "/api/v1/short-video/publish/ai-review"
    $review = Invoke-DyPost -Path $reviewPath -Body @{
        projectId = [long]$env:DY_VERIFY_PROJECT_ID
        title = "官方规则引用验收标题"
        platforms = @("douyin")
    }
    Assert-Field -Object $review -Name "officialReferenceRequired" -Path $reviewPath
    Assert-Field -Object $review -Name "officialReferenceSatisfied" -Path $reviewPath
    Assert-Field -Object $review -Name "officialReferenceStatus" -Path $reviewPath
    Write-Host "发布审核字段 OK: required=$($review.officialReferenceRequired), satisfied=$($review.officialReferenceSatisfied), status=$($review.officialReferenceStatus)"
} else {
    Write-Host "跳过发布审核接口：设置 DY_VERIFY_PROJECT_ID 后可验证 /short-video/publish/ai-review。"
}

Write-Host "官方规则引用闭环验收完成。"
