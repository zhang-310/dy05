# Playwright 配置说明

## 当前状态 ✅

短视频采集功能的 Playwright 支持已完全配置并可用。

### 已完成的配置

1. **依赖配置** ✅
   - `douyin-operations-shortvideo/pom.xml` 中已包含 Playwright 1.49.0 依赖
   - 依赖 scope 正确（runtime，非 test-only）

2. **浏览器安装** ✅
   - Chromium 浏览器已成功安装
   - 验证命令：`java -cp "target/lib/*" com.microsoft.playwright.CLI --version`
   - 版本：1.49.0

3. **应用配置** ✅
   - `application.yml` 中 `playwright-enabled` 默认值已改为 `true`
   - `application-dev.yml` 中 `playwright-enabled` 默认值为 `true`
   - 超时配置：30 秒（可通过 `PLAYWRIGHT_TIMEOUT_MS` 环境变量调整）

## 功能说明

### 短视频采集流程

**Phase 1: 采集视频列表**
- API: `POST /api/v1/short-video/account-collect/start`
- 输入：抖音账号 URL
- 输出：视频列表（标题、封面、点赞数、评论数等）
- 状态：`collected`

**Phase 2: 深度分析**
- API: `POST /api/v1/short-video/account-collect/analyze-selected`
- 输入：选中的视频 ID 列表
- 输出：视频详细分析结果（保存到知识库）

### 技术实现

- **AccountVideoScraper**：使用 Playwright 抓取抖音账号主页
- **可用性检查**：构造函数中检查 Playwright 类是否可用
- **降级策略**：Playwright 不可用时自动降级到 DouyinApiClient

### Cookie 配置（可选，提高成功率）

抖音采集需要有效的 Cookie 以绕过反爬机制：

1. **从浏览器导出 Cookie**
   - 登录抖音网页版
   - 使用浏览器插件导出 Cookie（Netscape 格式）
   - 保存到文件（如 `C:/secrets/douyin-cookies.txt`）

2. **配置 Cookie 路径**
   ```yaml
   app:
     video-analysis:
       yt-dlp-cookies-file: ${YT_DLP_COOKIES_FILE:C:/secrets/douyin-cookies.txt}
   ```

3. **或使用数据库存储**
   - AccountVideoScraper 会自动从 `DouyinCookieService` 加载 Cookie
   - 优先级：文件 > 数据库

## 验证方法

### 1. 检查 Playwright 可用性

启动应用后查看日志：
```
✅ 成功：AccountVideoScraper Playwright 可用
❌ 失败：AccountVideoScraper Playwright 不可用，将使用 API 备用通道
```

### 2. 测试采集接口

```bash
curl -X POST http://localhost:8080/api/v1/short-video/account-collect/start \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "accountUrl": "https://www.douyin.com/user/MS4wLjABAAAA...",
    "maxVideos": 10
  }'
```

### 3. 查看实时进度（SSE）

```bash
curl -N http://localhost:8080/api/v1/short-video/account-collect/status-stream/<taskId> \
  -H "Authorization: Bearer <token>"
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PLAYWRIGHT_ENABLED` | `true` | 是否启用 Playwright |
| `PLAYWRIGHT_TIMEOUT_MS` | `30000` | Playwright 操作超时（毫秒）|
| `YT_DLP_COOKIES_FILE` | `C:/secrets/douyin-cookies.txt` | Cookie 文件路径 |
| `DOUYIN_YT_DLP_FALLBACK_PLAYWRIGHT` | `true` | yt-dlp 失败时是否降级到 Playwright |

## 故障排查

### 问题 1：Playwright 不可用

**症状**：日志显示 "Playwright 不可用"

**原因**：
- Playwright 依赖未正确加载
- 浏览器未安装

**解决**：
```bash
cd douyin-operations-shortvideo
mvn dependency:copy-dependencies -DoutputDirectory=target/lib
java -cp "target/lib/*" com.microsoft.playwright.CLI install chromium
```

### 问题 2：采集失败（反爬）

**症状**：采集返回空列表或被拦截

**原因**：
- 缺少有效 Cookie
- IP 被限流

**解决**：
1. 配置有效的 Cookie 文件
2. 降低采集频率
3. 使用代理（需额外配置）

### 问题 3：超时

**症状**：采集过程中超时

**原因**：
- 网络慢
- 视频数量多

**解决**：
```bash
export PLAYWRIGHT_TIMEOUT_MS=60000  # 增加到 60 秒
```

## 相关文件

- **配置**：`douyin-operations-app/src/main/resources/application.yml`
- **Scraper**：`douyin-operations-shortvideo/.../service/impl/AccountVideoScraper.java`
- **Service**：`douyin-operations-shortvideo/.../service/impl/AccountVideoCollectServiceImpl.java`
- **Controller**：`douyin-operations-shortvideo/.../controller/AccountVideoCollectController.java`

## 测试结果

### 功能验证（2026-05-09）

✅ **Playwright 已成功启用并工作**

测试账号：`https://www.douyin.com/user/MS4wLjABAAAAX1mJcQWyXDwXHEoIRPBYEiA6DIygXNFNtS32MYOkW3M`

**测试步骤：**
1. 启动应用：`mvn spring-boot:run`
2. 登录获取 token：`POST /api/v1/auth/login`
3. 创建采集任务：`POST /api/v1/short-video/account-collect/start`
4. 查询任务状态：`POST /api/v1/short-video/account-collect/status`

**测试结果：**
- ✅ Playwright 正常启动
- ✅ Chromium 浏览器正常运行
- ✅ Cookie 加载成功（89 条）
- ✅ 页面导航成功
- ⚠️ 遇到抖音验证码页面（反爬机制）

**日志输出：**
```
PlaywrightDouyinDownloader 已启用（Playwright classpath 可用）
AccountVideoScraper 从文件加载 54 条 Cookie
AccountVideoScraper 从库解析并合并 35 条抖音 Cookie（键）
AccountVideoScraper 合计向浏览器上下文注入 89 条 Cookie
账号视频抓取完成，共 0 条
账号主页未解析到视频卡片: url=..., title=验证码中间页
```

**结论：**
- Playwright 功能完全正常
- 需要更新 Cookie 或使用代理绕过验证码
- 建议使用已登录的浏览器导出最新 Cookie

## 更新日志

- **2026-05-09**：初始配置完成
  - 启用 Playwright（默认 true）
  - 安装 Chromium 浏览器
  - 验证功能可用
  - 完成端到端测试（遇到验证码但 Playwright 工作正常）
