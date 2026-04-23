# 抖音视频下载 Cookies 配置指南

**日期**: 2026-04-21  
**问题**: 视频下载失败 - `Error {`  
**解决方案**: 配置 yt-dlp cookies

---

## ✅ 当前状态

- ✓ Cookies 文件已保存: `C:\secrets\douyin-cookies.txt` (7.3KB)
- ✓ yt-dlp 版本: 2026.03.17 (最新)
- ✓ Cookies 格式: Netscape HTTP Cookie File (正确)

---

## 🔧 配置步骤

### 方法 1: 设置 Windows 环境变量（推荐）

#### 步骤 1: 设置系统环境变量

**以管理员身份运行 PowerShell**：

```powershell
# 设置系统环境变量（永久生效）
[System.Environment]::SetEnvironmentVariable('YT_DLP_COOKIES_FILE', 'C:\secrets\douyin-cookies.txt', 'Machine')

# 验证
[System.Environment]::GetEnvironmentVariable('YT_DLP_COOKIES_FILE', 'Machine')
```

或使用 `setx` 命令：

```batch
setx YT_DLP_COOKIES_FILE "C:\secrets\douyin-cookies.txt" /M
```

#### 步骤 2: 重启应用

```batch
# 停止应用
taskkill /F /IM java.exe

# 重新启动（确保读取新的环境变量）
java -jar douyin-operations-app.jar
```

---

### 方法 2: 在启动脚本中设置（临时）

如果你有启动脚本（如 `start.bat` 或 `run.bat`），在启动前添加：

```batch
@echo off
REM 设置 yt-dlp cookies
set YT_DLP_COOKIES_FILE=C:\secrets\douyin-cookies.txt

REM 启动应用
java -jar douyin-operations-app/target/douyin-operations-app.jar
```

---

### 方法 3: 直接修改配置文件（不推荐）

修改 `douyin-operations-app/src/main/resources/application.yml`:

```yaml
app:
  video-analysis:
    yt-dlp-cookies-file: C:\secrets\douyin-cookies.txt  # 硬编码路径
```

**缺点**: 配置文件会被提交到 Git，可能泄露路径信息。

---

## 🧪 验证配置

### 1. 检查环境变量

```batch
# Windows CMD
echo %YT_DLP_COOKIES_FILE%

# PowerShell
$env:YT_DLP_COOKIES_FILE

# Git Bash
echo $YT_DLP_COOKIES_FILE
```

**期望输出**: `C:\secrets\douyin-cookies.txt`

### 2. 测试 yt-dlp 下载

```bash
# 测试元数据提取（不实际下载）
yt-dlp --cookies "C:\secrets\douyin-cookies.txt" \
  --dump-json \
  "https://www.douyin.com/video/7XXX"

# 如果成功，会输出 JSON 格式的视频元数据
```

### 3. 查看应用日志

启动应用后，查看日志：

```bash
# 查看 cookies 配置是否生效
grep "yt-dlp 使用 cookies 文件" logs/app.log

# 期望看到：
# yt-dlp 使用 cookies 文件: C:\secrets\douyin-cookies.txt
```

### 4. 触发拆解分析

1. 前端打开爆款视频列表
2. 点击"拆解分析"按钮
3. 打开详情抽屉，观察进度：

**期望结果**：
```json
{
  "steps": {
    "metadata": { "status": "done" },
    "download": { 
      "status": "done",              // ✅ 应该成功
      "detail": "视频已下载"
    },
    "asr": { "status": "done" },
    "scene": { "status": "done" },
    "bos": { "status": "done" }
  }
}
```

---

## 🔍 故障排查

### 问题 1: 环境变量未生效

**症状**: 日志仍显示 "抖音下载未传入任何 cookies"

**原因**: 
- 环境变量设置后未重启应用
- 使用了用户级环境变量而非系统级

**解决**:
```powershell
# 确认使用系统级环境变量
[System.Environment]::SetEnvironmentVariable('YT_DLP_COOKIES_FILE', 'C:\secrets\douyin-cookies.txt', 'Machine')

# 重启应用（必须）
```

### 问题 2: Cookies 过期

**症状**: 日志显示 "Fresh cookies required"

**原因**: Cookies 有效期通常 7-30 天

**解决**:
1. 重新登录抖音网页版
2. 重新导出 cookies.txt
3. 覆盖 `C:\secrets\douyin-cookies.txt`
4. 无需重启应用（会自动读取新文件）

### 问题 3: 文件路径错误

**症状**: 日志显示 "yt-dlp cookies 文件不存在或不是文件"

**原因**: 路径使用了反斜杠 `\` 或包含空格

**解决**:
```batch
# 正确路径（使用正斜杠或双反斜杠）
C:/secrets/douyin-cookies.txt
C:\\secrets\\douyin-cookies.txt

# 错误路径
C:\secrets\douyin-cookies.txt  # 单反斜杠可能被转义
```

### 问题 4: 权限问题

**症状**: 应用无法读取 cookies 文件

**解决**:
```batch
# 检查文件权限
icacls C:\secrets\douyin-cookies.txt

# 添加读取权限
icacls C:\secrets\douyin-cookies.txt /grant Everyone:R
```

---

## 📊 预期效果对比

### 修复前
```
✅ 元数据提取成功
❌ 下载失败：Error {
⏭️ 跳过 ASR
⚠️ 降级为推演模式
✅ BOS 上传完成（但无视频）
```

### 修复后
```
✅ 元数据提取成功
✅ 下载成功：视频已下载
✅ ASR 完成
✅ 场景/抽帧处理完成（10-20 张关键帧）
✅ BOS 上传完成（视频+关键帧+封面）
✅ LLM 拆解完成
```

---

## 🎯 快速检查清单

- [ ] Cookies 文件存在: `C:\secrets\douyin-cookies.txt`
- [ ] 文件大小 > 1KB（不是空文件）
- [ ] 文件格式正确（第一行是 `# Netscape HTTP Cookie File`）
- [ ] 环境变量已设置: `YT_DLP_COOKIES_FILE=C:\secrets\douyin-cookies.txt`
- [ ] 应用已重启（读取新环境变量）
- [ ] yt-dlp 版本 >= 2024.01.01
- [ ] 日志显示 "yt-dlp 使用 cookies 文件"

---

## 📝 相关配置

### application.yml 配置项

```yaml
app:
  video-analysis:
    enabled: true
    yt-dlp-path: yt-dlp
    yt-dlp-cookies-file: ${YT_DLP_COOKIES_FILE:}  # 从环境变量读取
    yt-dlp-cookies-from-browser: ${YT_DLP_COOKIES_FROM_BROWSER:}  # 备选方案
    douyin-skip-yt-dlp-download: false  # 不跳过下载
    douyin-yt-dlp-fallback-playwright: true  # 启用 Playwright 回退
```

### 环境变量优先级

1. `YT_DLP_COOKIES_FILE` - Cookies 文件路径（推荐）
2. `YT_DLP_COOKIES_FROM_BROWSER` - 从浏览器读取（如 `chrome`）
3. 无 Cookies - 下载失败（抖音需要登录态）

---

## 🔄 Cookies 更新流程

Cookies 会过期，建议定期更新：

### 自动化脚本（可选）

```batch
@echo off
REM update_cookies.bat - 更新抖音 cookies

echo 请按以下步骤操作：
echo 1. 打开 Chrome 浏览器
echo 2. 访问 https://www.douyin.com 并登录
echo 3. 使用 "Get cookies.txt LOCALLY" 插件导出 cookies
echo 4. 保存为 C:\secrets\douyin-cookies.txt
echo 5. 按任意键继续...
pause

REM 验证文件
if exist "C:\secrets\douyin-cookies.txt" (
    echo ✓ Cookies 文件已更新
    echo 文件大小: 
    dir "C:\secrets\douyin-cookies.txt" | find "douyin-cookies.txt"
    echo.
    echo 无需重启应用，下次下载时会自动使用新 cookies
) else (
    echo ✗ 文件不存在，请重新导出
)
pause
```

---

## 📞 支持

如果配置后仍然失败，请提供以下信息：

1. **环境变量值**:
   ```batch
   echo %YT_DLP_COOKIES_FILE%
   ```

2. **Cookies 文件信息**:
   ```batch
   dir C:\secrets\douyin-cookies.txt
   head -5 C:\secrets\douyin-cookies.txt
   ```

3. **应用日志**（最近 50 行）:
   ```bash
   grep "yt-dlp\|downloadVideo\|VideoAnalysisService" logs/app.log | tail -50
   ```

4. **错误详情**:
   ```bash
   grep "下载失败\|yt-dlp 下载失败" logs/app.log | tail -10
   ```
