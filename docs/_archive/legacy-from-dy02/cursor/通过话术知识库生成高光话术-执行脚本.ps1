# 通过话术知识库生成李阳阳高光话术
# 前置：1) 后端已启动 (mvn spring-boot:run)  2) 已登录获取 token
# 用法：$token = "你的JWT"; .\通过话术知识库生成高光话术-执行脚本.ps1

param(
    [string]$Token = $env:HUASHU_TOKEN,
    [string]$HostName = "李阳阳",
    [string]$SourcePath = "C:\Users\Administrator\Desktop\01",
    [string]$OutputFile = "李阳阳-高光话术-知识库生成.md"
)

if (-not $Token) {
    Write-Host "请设置 token：`$env:HUASHU_TOKEN = '你的JWT' 或传入 -Token 参数"
    Write-Host "可从浏览器登录后，F12 -> Application -> Local Storage 中获取"
    exit 1
}

$base = "http://localhost:8080/api/v1"
$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type"  = "application/json"
}

# 1. 生成（含可选导入）
$body = @{
    hostName   = $HostName
    sourcePath = $SourcePath
} | ConvertTo-Json -Compress

Write-Host "正在调用话术知识库生成接口..."
try {
    $resp = Invoke-RestMethod -Uri "$base/ai/knowledge-base/huashu/generate-highlight" `
        -Method Post -Headers $headers -Body $body -ContentType "application/json; charset=utf-8"
} catch {
    Write-Host "请求失败: $_"
    exit 1
}

# RESTResult: { status, data: { content, ragRefCount, ... } }
$payload = if ($resp.data) { $resp.data } else { $resp }
$content = $payload.content
if (-not $content) {
    Write-Host "未获取到生成内容"
    exit 1
}

# 2. 写入文件
$content | Out-File -FilePath $OutputFile -Encoding utf8
Write-Host "已写入: $OutputFile"
Write-Host "RAG 引用数: $($payload.ragRefCount)"
if ($payload.importSummary) {
    Write-Host "导入: 成功 $($payload.importSummary.success) / 失败 $($payload.importSummary.failed)"
}
