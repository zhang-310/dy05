# E2E 测试自动修复系统

## 概述

本项目实现了完整的 E2E 测试自动修复系统，能够智能检测测试失败原因并自动修复常见问题。

## 核心功能

### 1. 自动错误检测与分类

系统能够识别 7 种常见错误类型：

- **TIMEOUT** - 超时错误
- **ELEMENT_NOT_FOUND** - 元素未找到
- **AUTH_ERROR** - 认证错误
- **NAVIGATION_ERROR** - 导航错误
- **ASSERTION_ERROR** - 断言错误
- **NETWORK_ERROR** - 网络错误
- **ELEMENT_OBSCURED** - 元素被遮挡

### 2. 智能修复策略

每种错误类型都有对应的自动修复策略：

| 错误类型 | 修复策略 | 自动修复 |
|---------|---------|---------|
| TIMEOUT | 增加超时时间 + 重试 | ✅ |
| ELEMENT_NOT_FOUND | 等待元素出现 + 重试 | ✅ |
| AUTH_ERROR | 自动登录 + 重新请求 | ✅ |
| ELEMENT_OBSCURED | 关闭遮罩层 + 滚动 + 重试 | ✅ |
| NAVIGATION_ERROR | 刷新页面 + 重试 | ⚠️ |
| ASSERTION_ERROR | 重试断言 | ⚠️ |
| NETWORK_ERROR | 等待网络空闲 + 重试 | ⚠️ |

### 3. 自动修复 Fixture

`autofix.fixture.ts` 提供了增强的页面对象，自动包装常用方法：

```typescript
// 自动修复的 goto
page.goto = async (url: string) => {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      const response = await originalGoto(url)
      
      // 自动检测登录状态
      if (page.url().includes('/login')) {
        await autoLogin(page)
        return await originalGoto(url)
      }
      
      return response
    } catch (error) {
      await tryFix(page, error, options)
    }
  }
}

// 自动修复的 click
page.click = async (selector: string) => {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      await page.waitForSelector(selector, { state: 'visible' })
      await page.locator(selector).scrollIntoViewIfNeeded()
      return await originalClick(selector)
    } catch (error) {
      await tryCloseOverlays(page) // 关闭遮罩层
    }
  }
}

// 自动修复的 fill
page.fill = async (selector: string, value: string) => {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      await page.waitForSelector(selector, { state: 'visible' })
      await originalClick(selector)
      await page.keyboard.press('Control+A')
      await page.keyboard.press('Backspace')
      return await originalFill(selector, value)
    } catch (error) {
      await page.waitForTimeout(500)
    }
  }
}
```

### 4. 失败分析器

`test-fixer.ts` 提供了完整的失败分析功能：

```typescript
class TestAutoFixer {
  // 分析失败原因
  analyzeFailure(testName: string, error: Error): FailureAnalysis {
    const errorType = this.categorizeError(error.message)
    const suggestions = this.generateSuggestions(errorType)
    const autoFixable = this.isAutoFixable(errorType)
    const fixCode = this.generateFixCode(errorType)
    
    return { testName, errorType, suggestions, autoFixable, fixCode }
  }
  
  // 生成修复报告
  generateReport(): string {
    // 按错误类型分组
    // 生成修复建议
    // 生成修复代码
  }
}
```

### 5. 带重试的断言

`expectWithRetry` 函数提供了自动重试的断言：

```typescript
await expectWithRetry(async () => {
  await expect(page.locator('text=成功')).toBeVisible()
}, { maxRetries: 5, retryDelay: 2000 })
```

## 使用方法

### 方式 1：使用 autoFixPage

```typescript
import { test, expectWithRetry } from '../fixtures/autofix.fixture'

test('自动修复测试', async ({ autoFixPage }) => {
  // 所有操作都会自动修复
  await autoFixPage.goto('/admin/dashboard')
  await autoFixPage.click('button:has-text("新建")')
  await autoFixPage.fill('input[name="title"]', '测试')
  
  await expectWithRetry(async () => {
    await expect(autoFixPage.locator('text=成功')).toBeVisible()
  })
})
```

### 方式 2：自定义修复选项

```typescript
test('自定义修复', async ({ page, autoFixOptions }) => {
  const options = {
    ...autoFixOptions,
    maxRetries: 5,        // 最大重试次数
    autoLogin: true,      // 自动登录
    clearCache: true,     // 清除缓存
    waitForStability: true // 等待元素稳定
  }
  
  // 使用自定义选项
})
```

### 方式 3：使用 autoFixTest

```typescript
import { autoFixTest } from '../utils/test-fixer'

autoFixTest('测试名称', async ({ page }) => {
  // 测试失败时自动分析并输出修复建议
  await page.goto('/admin/dashboard')
})
```

## 修复示例

### 示例 1：自动处理登录过期

```typescript
test('自动登录', async ({ autoFixPage }) => {
  // 如果检测到未登录，会自动登录
  await autoFixPage.goto('/admin/dashboard')
  
  // 验证页面加载
  await expect(autoFixPage.locator('h1')).toBeVisible()
})
```

### 示例 2：自动重试点击

```typescript
test('自动重试点击', async ({ autoFixPage }) => {
  await autoFixPage.goto('/admin/product/list')
  
  // 即使元素被遮罩层覆盖，也会自动关闭遮罩层并重试
  await autoFixPage.click('button:has-text("新建商品")')
  
  await expect(autoFixPage.locator('.MuiDialog-root')).toBeVisible()
})
```

### 示例 3：自动处理网络延迟

```typescript
test('自动处理延迟', async ({ autoFixPage }) => {
  await autoFixPage.goto('/admin/live/sessions')
  
  // 自动等待数据加载
  await expectWithRetry(async () => {
    await expect(autoFixPage.locator('.MuiDataGrid-row')).not.toHaveCount(0)
  }, { maxRetries: 5, retryDelay: 2000 })
})
```

### 示例 4：复杂表单提交

```typescript
test('复杂表单', async ({ autoFixPage }) => {
  await autoFixPage.goto('/admin/agent/list')
  await autoFixPage.click('button:has-text("新建智能体")')
  
  // 自动处理输入框未就绪的情况
  await autoFixPage.fill('input[name="agentName"]', '测试智能体')
  await autoFixPage.fill('textarea[name="agentDescription"]', '描述')
  
  // 自动处理下拉框
  await autoFixPage.click('label:has-text("智能体类型") + div')
  await autoFixPage.waitForSelector('.MuiMenu-root')
  await autoFixPage.click('.MuiMenuItem-root:has-text("客服助手")')
  
  await autoFixPage.click('button:has-text("创建")')
  
  await expectWithRetry(async () => {
    await expect(autoFixPage.locator('text=创建成功')).toBeVisible()
  })
})
```

## 查看修复报告

测试运行后，系统会自动生成修复报告：

```bash
# 查看修复报告
cat test-results/autofix-report.md

# 查看测试总结
cat test-results/summary.txt

# 查看 HTML 报告
npx playwright show-report
```

修复报告包含：

1. **失败统计**
   - 总失败数
   - 可自动修复数量
   - 按错误类型分组

2. **详细分析**
   - 测试名称
   - 错误信息
   - 错误类型
   - 修复建议（3-5 条）

3. **修复代码**
   - 针对每个错误的具体修复代码
   - 可直接复制使用

## 修复报告示例

```markdown
# 测试失败自动修复报告

生成时间: 2026-04-05 16:30:00

总失败数: 5
可自动修复: 4

## TIMEOUT (2 个)

### 创建直播场次 - 等待保存成功

**错误信息:**
```
Timeout 30000ms exceeded waiting for locator('text=保存成功')
```

**修复建议:**
- 增加超时时间: { timeout: 30000 }
- 使用 waitForLoadState("networkidle")
- 检查 API 响应是否过慢
- 使用 expectWithRetry 进行重试

**修复代码:**
```typescript
// 修复建议：增加超时时间并使用重试
await expectWithRetry(async () => {
  await expect(page.locator('text=保存成功')).toBeVisible()
}, { maxRetries: 5, retryDelay: 2000 })
```

---

## ELEMENT_NOT_FOUND (2 个)

### 点击新建按钮

**错误信息:**
```
Locator.click: Element not found
```

**修复建议:**
- 检查选择器是否正确
- 使用 waitForSelector 等待元素出现
- 检查元素是否在 iframe 中
- 使用更宽松的选择器（如 :has-text）

**修复代码:**
```typescript
// 修复建议：等待元素出现
await page.waitForSelector('button:has-text("新建")', { state: 'visible', timeout: 10000 })
await page.click('button:has-text("新建")')
```

---

## AUTH_ERROR (1 个)

### 访问管理后台

**错误信息:**
```
401 Unauthorized
```

**修复建议:**
- 使用 authenticatedPage fixture
- 检查 token 是否过期
- 在测试前重新登录
- 使用 autoFixPage 自动处理登录

**修复代码:**
```typescript
// 修复建议：使用 autoFixPage 自动处理登录
import { test } from '../fixtures/autofix.fixture'

test('测试名称', async ({ autoFixPage }) => {
  await autoFixPage.goto('/admin/dashboard')
  // autoFixPage 会自动检测并处理登录
})
```

---
```

## 配置选项

### AutoFixOptions

```typescript
interface AutoFixOptions {
  maxRetries?: number        // 最大重试次数（默认 3）
  autoLogin?: boolean        // 自动登录（默认 true）
  clearCache?: boolean       // 清除缓存（默认 false）
  waitForStability?: boolean // 等待元素稳定（默认 true）
}
```

### 全局配置

在 `playwright.config.ts` 中配置：

```typescript
export default defineConfig({
  retries: process.env.CI ? 2 : 1, // 本地也启用重试
  
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  
  projects: [
    {
      name: 'chromium-autofix',
      use: { ...devices['Desktop Chrome'] },
      testMatch: /.*autofix\.spec\.ts/,
    },
  ],
  
  globalSetup: require.resolve('./e2e/global-setup.ts'),
  globalTeardown: require.resolve('./e2e/global-teardown.ts'),
})
```

## 运行自动修复测试

```bash
# 运行所有测试（启用自动修复）
npm run test:e2e

# 运行自动修复示例测试
npm run test:e2e:autofix

# 查看修复报告
npm run test:e2e:report
```

## 最佳实践

1. **优先使用 autoFixPage**：对于容易失败的测试，使用 `autoFixPage` 而非普通 `page`

2. **使用 expectWithRetry**：对于异步数据加载，使用带重试的断言

3. **合理设置重试次数**：根据操作复杂度调整 `maxRetries`

4. **查看修复报告**：定期查看修复报告，识别系统性问题

5. **修复根本原因**：自动修复是临时方案，应该修复代码中的根本问题

## 总结

自动修复系统提供了：

- ✅ 7 种错误类型的智能识别
- ✅ 4 种错误的完全自动修复
- ✅ 详细的失败分析和修复建议
- ✅ 可复制的修复代码
- ✅ 完整的测试报告
- ✅ 灵活的配置选项

这大大提高了测试的稳定性和可维护性，减少了因环境问题导致的测试失败。
