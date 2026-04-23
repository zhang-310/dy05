# E2E 测试快速参考

## 快速命令

```bash
# 运行所有测试
npm run test:e2e

# UI 模式（推荐）
npm run test:e2e:ui

# 调试模式
npm run test:e2e:debug

# 查看报告
npm run test:e2e:report

# 自动修复测试
npm run test:e2e:autofix

# 生成测试代码
npm run test:e2e:codegen
```

## 测试模板

### 基础测试

```typescript
import { test, expect } from '@playwright/test'
import { authenticatedPage } from '../fixtures/auth.fixture'

test.describe('模块名称', () => {
  test.use({ storageState: authenticatedPage })

  test('测试用例', async ({ page }) => {
    await page.goto('/admin/module/page')
    await page.waitForLoadState('networkidle')
    
    await page.click('button:has-text("操作")')
    await page.fill('input[name="field"]', 'value')
    
    await expect(page.locator('text=成功')).toBeVisible()
  })
})
```

### 自动修复测试

```typescript
import { test, expectWithRetry } from '../fixtures/autofix.fixture'

test('自动修复', async ({ autoFixPage }) => {
  await autoFixPage.goto('/admin/page')
  await autoFixPage.click('button:has-text("操作")')
  await autoFixPage.fill('input[name="field"]', 'value')
  
  await expectWithRetry(async () => {
    await expect(autoFixPage.locator('text=成功')).toBeVisible()
  })
})
```

### Page Object 测试

```typescript
import { test } from '@playwright/test'
import { LiveSessionsPage } from '../utils/page-objects'

test('使用 POM', async ({ page }) => {
  const livePage = new LiveSessionsPage(page)
  
  await livePage.goto()
  await livePage.createSession('标题', '描述')
  await livePage.searchSession('标题')
})
```

## 常用选择器

```typescript
// 按文本
page.locator('button:has-text("保存")')
page.locator('text=保存成功')

// 按属性
page.locator('input[name="username"]')
page.locator('button[aria-label="关闭"]')

// 按类名
page.locator('.MuiDialog-root')
page.locator('.MuiDataGrid-row')

// 组合选择器
page.locator('label:has-text("类型") + div')
page.locator('.MuiDataGrid-row:has-text("测试")')

// 第 N 个元素
page.locator('button').first()
page.locator('button').nth(2)
page.locator('button').last()
```

## 常用操作

```typescript
// 导航
await page.goto('/admin/dashboard')
await page.waitForLoadState('networkidle')

// 点击
await page.click('button:has-text("保存")')
await page.locator('button').click()

// 填写
await page.fill('input[name="title"]', '测试')
await page.locator('textarea').fill('内容')

// 选择下拉框
await page.click('label:has-text("类型") + div')
await page.waitForSelector('.MuiMenu-root')
await page.click('.MuiMenuItem-root:has-text("选项")')

// 等待
await page.waitForSelector('button', { state: 'visible' })
await page.waitForTimeout(1000)
await page.waitForResponse(resp => resp.url().includes('/api'))

// 断言
await expect(page.locator('text=成功')).toBeVisible()
await expect(page.locator('input')).toHaveValue('测试')
await expect(page).toHaveURL(/\/admin/)
```

## 测试辅助函数

```typescript
import {
  waitForPageLoad,
  fillForm,
  expectToast,
  selectOption,
  clickRowAction,
  waitForDialog,
  closeDialog,
  searchAndVerify,
  goToNextPage,
  selectRows,
} from '../utils/test-helpers'

// 等待页面加载
await waitForPageLoad(page)

// 填写表单
await fillForm(page, {
  username: 'test',
  email: 'test@example.com'
})

// 验证 Toast
await expectToast(page, '保存成功')

// 选择下拉框
await selectOption(page, '类型', '选项')

// 点击表格行操作
await clickRowAction(page, '测试数据', '编辑')

// 等待对话框
await waitForDialog(page)
await closeDialog(page)

// 搜索
await searchAndVerify(page, '关键词', '预期结果')

// 翻页
await goToNextPage(page)

// 批量选择
await selectRows(page, 3)
```

## 调试技巧

```typescript
// 暂停执行
await page.pause()

// 截图
await page.screenshot({ path: 'debug.png' })

// 打印信息
console.log(await page.locator('button').textContent())
console.log(await page.locator('button').isVisible())

// 等待观察
await page.waitForTimeout(5000)

// 查看 HTML
console.log(await page.content())
```

## 错误处理

```typescript
// try-catch
try {
  await page.click('button', { timeout: 2000 })
} catch {
  // 忽略错误
}

// 条件检查
if (await page.locator('.dialog').isVisible()) {
  await page.click('button:has-text("关闭")')
}

// 使用自动修复
import { test } from '../fixtures/autofix.fixture'

test('自动处理错误', async ({ autoFixPage }) => {
  // 自动重试、自动登录、自动关闭遮罩层
  await autoFixPage.goto('/admin/page')
})
```

## 常见问题

### 元素未找到

```typescript
// ❌ 错误
await page.click('button')

// ✅ 正确
await page.waitForSelector('button:has-text("保存")', { timeout: 10000 })
await page.click('button:has-text("保存")')
```

### 元素被遮挡

```typescript
// ❌ 错误
await page.click('button')

// ✅ 正确
await page.locator('button').scrollIntoViewIfNeeded()
await page.keyboard.press('Escape') // 关闭遮罩
await page.click('button')
```

### 超时

```typescript
// ❌ 错误
await page.waitForSelector('button')

// ✅ 正确
await page.waitForSelector('button', { timeout: 30000 })

// 或使用重试
await expectWithRetry(async () => {
  await expect(page.locator('button')).toBeVisible()
}, { maxRetries: 5, retryDelay: 2000 })
```

### 认证失败

```typescript
// ❌ 错误
test('测试', async ({ page }) => {
  await page.goto('/admin/dashboard') // 401
})

// ✅ 正确
import { authenticatedPage } from '../fixtures/auth.fixture'

test.use({ storageState: authenticatedPage })

test('测试', async ({ page }) => {
  await page.goto('/admin/dashboard') // 已登录
})

// 或使用自动修复
test('测试', async ({ autoFixPage }) => {
  await autoFixPage.goto('/admin/dashboard') // 自动登录
})
```

## 测试覆盖

| 模块 | 测试文件 | 测试数 |
|------|---------|--------|
| 认证 | auth.spec.ts | 15 |
| 直播 | live.spec.ts | 25 |
| 短视频 | shortvideo.spec.ts | 20 |
| 商品 | product.spec.ts | 14 |
| 话术 | script.spec.ts | 20 |
| 文案 | copy.spec.ts | 15 |
| 智能体 | agent.spec.ts | 15 |
| AI | ai.spec.ts | 12 |
| 系统 | system.spec.ts | 24 |
| 抖音 | douyin.spec.ts | 14 |
| 仪表盘 | dashboard.spec.ts | 20 |
| **总计** | **11 个文件** | **169** |

## 报告位置

```
test-results/
├── results.json           # JSON 结果
├── junit.xml             # JUnit XML（CI）
├── summary.txt           # 测试总结
├── autofix-report.md     # 自动修复报告
└── autofix-log.json      # 失败日志

playwright-report/
└── index.html            # HTML 报告
```

## 相关文档

- [完整测试指南](./E2E-TESTING-GUIDE.md)
- [自动修复系统](./AUTOFIX-SYSTEM.md)
- [Playwright 文档](https://playwright.dev)
