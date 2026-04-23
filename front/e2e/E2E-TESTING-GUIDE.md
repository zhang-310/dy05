# E2E 测试完整指南

## 目录

- [概述](#概述)
- [快速开始](#快速开始)
- [测试架构](#测试架构)
- [自动修复功能](#自动修复功能)
- [编写测试](#编写测试)
- [运行测试](#运行测试)
- [调试测试](#调试测试)
- [CI/CD 集成](#cicd-集成)
- [最佳实践](#最佳实践)
- [故障排查](#故障排查)

## 概述

本项目使用 Playwright 进行 E2E 测试，覆盖所有 128 个前端页面。测试框架包含：

- ✅ **完整覆盖**：11 个模块，169 个测试用例
- 🔧 **自动修复**：智能检测并修复常见测试失败
- 📊 **多浏览器**：Chromium、Firefox、WebKit、移动端
- 🎯 **Page Object**：可维护的页面对象模式
- 📈 **详细报告**：HTML、JSON、JUnit、自动修复报告

## 快速开始

### 安装依赖

```bash
cd front
npm install
npx playwright install
```

### 运行所有测试

```bash
npm run test:e2e
```

### 运行单个测试文件

```bash
npx playwright test e2e/tests/live.spec.ts
```

### 运行自动修复测试

```bash
npx playwright test e2e/tests/autofix.spec.ts --project=chromium-autofix
```

### 查看测试报告

```bash
npx playwright show-report
```

## 测试架构

### 目录结构

```
e2e/
├── fixtures/
│   ├── auth.fixture.ts          # 认证 fixture（自动登录）
│   └── autofix.fixture.ts       # 自动修复 fixture
├── tests/
│   ├── auth.spec.ts             # 认证模块测试（15 个）
│   ├── live.spec.ts             # 直播模块测试（25 个）
│   ├── shortvideo.spec.ts       # 短视频模块测试（20 个）
│   ├── product.spec.ts          # 商品模块测试（14 个）
│   ├── script.spec.ts           # 话术模块测试（20 个）
│   ├── copy.spec.ts             # 文案模块测试（15 个）
│   ├── agent.spec.ts            # 智能体模块测试（15 个）
│   ├── ai.spec.ts               # AI 模块测试（12 个）
│   ├── system.spec.ts           # 系统模块测试（24 个）
│   ├── douyin.spec.ts           # 抖音模块测试（14 个）
│   ├── dashboard.spec.ts        # 仪表盘模块测试（20 个）
│   └── autofix.spec.ts          # 自动修复示例测试
├── utils/
│   ├── test-helpers.ts          # 测试辅助函数
│   ├── test-fixer.ts            # 自动修复分析器
│   └── page-objects/            # 页面对象
│       ├── BasePage.ts          # 基础页面类
│       └── index.ts             # 具体页面对象
├── global-setup.ts              # 全局设置
├── global-teardown.ts           # 全局清理
└── README.md                    # 本文档
```

### 测试统计

| 模块 | 测试数 | 覆盖页面 |
|------|--------|----------|
| 认证 | 15 | 登录、注册、权限 |
| 直播 | 25 | 场次、话术、审批 |
| 短视频 | 20 | 项目、脚本、素材 |
| 商品 | 14 | 商品、分类、评分 |
| 话术 | 20 | 生成、优化、合规 |
| 文案 | 15 | 文案库、模板、分析 |
| 智能体 | 15 | 智能体、对话、配置 |
| AI | 12 | 知识库、进化、提示词 |
| 系统 | 24 | 用户、角色、监控 |
| 抖音 | 14 | 账号、数据、分析 |
| 仪表盘 | 20 | 报表、支付、工作流 |
| **总计** | **169** | **128 页面** |

## 自动修复功能

### 功能概述

自动修复系统能够：

1. **自动检测**：识别 7 种常见错误类型
2. **智能重试**：自动重试失败的操作
3. **自动登录**：检测到未登录时自动登录
4. **关闭遮罩**：自动关闭阻挡元素的遮罩层
5. **生成建议**：为每个失败提供修复建议和代码
6. **生成报告**：汇总所有失败并生成修复报告

### 错误类型

| 错误类型 | 自动修复 | 修复策略 |
|---------|---------|---------|
| TIMEOUT | ✅ | 增加超时 + 重试 |
| ELEMENT_NOT_FOUND | ✅ | 等待元素 + 重试 |
| AUTH_ERROR | ✅ | 自动登录 |
| ELEMENT_OBSCURED | ✅ | 关闭遮罩层 + 滚动 |
| NAVIGATION_ERROR | ⚠️ | 刷新页面 |
| ASSERTION_ERROR | ⚠️ | 重试断言 |
| NETWORK_ERROR | ⚠️ | 重试请求 |

### 使用自动修复

#### 方式 1：使用 autoFixPage fixture

```typescript
import { test, expectWithRetry } from '../fixtures/autofix.fixture'

test('自动修复示例', async ({ autoFixPage }) => {
  // autoFixPage 会自动处理常见问题
  await autoFixPage.goto('/admin/dashboard')
  await autoFixPage.click('button:has-text("新建")')
  await autoFixPage.fill('input[name="title"]', '测试')
  
  // 使用带重试的断言
  await expectWithRetry(async () => {
    await expect(autoFixPage.locator('text=成功')).toBeVisible()
  })
})
```

#### 方式 2：自定义重试选项

```typescript
test('自定义重试', async ({ page, autoFixOptions }) => {
  const customOptions = {
    ...autoFixOptions,
    maxRetries: 5,
    clearCache: true,
    waitForStability: true,
  }
  
  // 使用自定义选项
  await page.goto('/admin/dashboard')
})
```

### 查看修复报告

测试运行后，查看自动生成的修复报告：

```bash
cat test-results/autofix-report.md
```

报告包含：
- 失败统计
- 错误分类
- 修复建议
- 修复代码示例

## 编写测试

### 基础测试模板

```typescript
import { test, expect } from '@playwright/test'
import { authenticatedPage } from '../fixtures/auth.fixture'

test.describe('模块名称', () => {
  test.use({ storageState: authenticatedPage })

  test('测试用例名称', async ({ page }) => {
    // 1. 导航到页面
    await page.goto('/admin/module/page')
    
    // 2. 等待页面加载
    await page.waitForLoadState('networkidle')
    
    // 3. 执行操作
    await page.click('button:has-text("新建")')
    await page.fill('input[name="title"]', '测试标题')
    await page.click('button:has-text("保存")')
    
    // 4. 验证结果
    await expect(page.locator('text=保存成功')).toBeVisible()
  })
})
```

### 使用 Page Object

```typescript
import { test } from '@playwright/test'
import { LiveSessionsPage } from '../utils/page-objects'

test('使用 Page Object', async ({ page }) => {
  const livePage = new LiveSessionsPage(page)
  
  await livePage.goto()
  await livePage.createSession('测试场次', '测试描述')
  await livePage.searchSession('测试场次')
})
```

### 使用测试辅助函数

```typescript
import { test } from '@playwright/test'
import {
  waitForPageLoad,
  fillForm,
  expectToast,
  selectOption,
  clickRowAction,
} from '../utils/test-helpers'

test('使用辅助函数', async ({ page }) => {
  await page.goto('/admin/product/list')
  await waitForPageLoad(page)
  
  await page.click('button:has-text("新建商品")')
  
  await fillForm(page, {
    productName: '测试商品',
    price: '99.99',
  })
  
  await selectOption(page, '分类', '护肤品')
  
  await page.click('button:has-text("保存")')
  await expectToast(page, '保存成功')
})
```

## 运行测试

### 基本命令

```bash
# 运行所有测试
npm run test:e2e

# 运行单个文件
npx playwright test e2e/tests/live.spec.ts

# 运行特定测试
npx playwright test -g "创建直播场次"

# 运行特定浏览器
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit

# 运行移动端测试
npx playwright test --project=mobile-chrome
npx playwright test --project=mobile-safari
```

### 调试模式

```bash
# UI 模式（推荐）
npx playwright test --ui

# 调试模式
npx playwright test --debug

# 调试特定测试
npx playwright test e2e/tests/live.spec.ts --debug

# 慢速模式（便于观察）
npx playwright test --headed --slow-mo=1000
```

### 查看报告

```bash
# HTML 报告
npx playwright show-report

# 查看测试结果 JSON
cat test-results/results.json

# 查看 JUnit XML（CI 集成）
cat test-results/junit.xml

# 查看自动修复报告
cat test-results/autofix-report.md

# 查看测试总结
cat test-results/summary.txt
```

## 调试测试

### 使用 Playwright Inspector

```bash
npx playwright test --debug
```

功能：
- 单步执行
- 查看元素选择器
- 查看控制台日志
- 查看网络请求

### 使用 Trace Viewer

```bash
# 运行测试并记录 trace
npx playwright test --trace on

# 查看 trace
npx playwright show-trace test-results/.../trace.zip
```

### 截图和视频

配置已启用失败时自动截图和录制视频：

```typescript
// playwright.config.ts
use: {
  screenshot: 'only-on-failure',
  video: 'retain-on-failure',
  trace: 'retain-on-failure',
}
```

查看失败截图：
```bash
ls test-results/*/test-failed-*.png
```

### 常用调试技巧

```typescript
// 1. 暂停执行
await page.pause()

// 2. 打印元素信息
const element = page.locator('button')
console.log(await element.textContent())
console.log(await element.isVisible())

// 3. 等待特定时间（调试用）
await page.waitForTimeout(5000)

// 4. 截图
await page.screenshot({ path: 'debug.png' })

// 5. 查看页面 HTML
console.log(await page.content())
```

## CI/CD 集成

### GitHub Actions 示例

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install dependencies
        run: |
          cd front
          npm ci
          npx playwright install --with-deps
      
      - name: Run E2E tests
        run: |
          cd front
          npm run test:e2e
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: front/playwright-report/
      
      - name: Upload test artifacts
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: front/test-results/
```

### Docker 运行

```bash
# 使用 Playwright Docker 镜像
docker run -it --rm \
  -v $(pwd):/work \
  -w /work/front \
  mcr.microsoft.com/playwright:v1.40.0-focal \
  npm run test:e2e
```

## 最佳实践

### 1. 测试独立性

每个测试应该独立运行，不依赖其他测试：

```typescript
// ❌ 错误：依赖前一个测试
test('创建商品', async ({ page }) => {
  // 创建商品
})

test('编辑商品', async ({ page }) => {
  // 假设商品已存在 - 依赖前一个测试
})

// ✅ 正确：每个测试独立
test('编辑商品', async ({ page }) => {
  // 先创建商品
  await createProduct(page, '测试商品')
  // 再编辑
  await editProduct(page, '测试商品', '新价格')
})
```

### 2. 使用有意义的选择器

```typescript
// ❌ 避免：脆弱的选择器
await page.click('.MuiButton-root:nth-child(3)')

// ✅ 推荐：语义化选择器
await page.click('button:has-text("保存")')
await page.click('button[aria-label="保存"]')
await page.click('[data-testid="save-button"]')
```

### 3. 等待策略

```typescript
// ❌ 避免：固定等待
await page.waitForTimeout(5000)

// ✅ 推荐：等待特定条件
await page.waitForSelector('button:has-text("保存")')
await page.waitForLoadState('networkidle')
await page.waitForResponse(resp => resp.url().includes('/api/save'))
```

### 4. 错误处理

```typescript
// ✅ 使用 try-catch 处理可选操作
try {
  await page.click('button:has-text("关闭")', { timeout: 2000 })
} catch {
  // 对话框可能已关闭，忽略错误
}

// ✅ 使用条件检查
if (await page.locator('.MuiDialog-root').isVisible()) {
  await page.click('button:has-text("关闭")')
}
```

### 5. 测试数据管理

```typescript
// ✅ 使用唯一标识符
const timestamp = Date.now()
const productName = `测试商品-${timestamp}`

// ✅ 测试后清理
test('创建商品', async ({ page }) => {
  const productName = `测试商品-${Date.now()}`
  
  await createProduct(page, productName)
  
  // 清理
  await deleteProduct(page, productName)
})
```

## 故障排查

### 常见问题

#### 1. 元素未找到

```
Error: Locator.click: Timeout 30000ms exceeded.
```

**解决方案**：
- 检查选择器是否正确
- 增加等待时间
- 使用 `waitForSelector`
- 检查元素是否在 iframe 中

```typescript
// 等待元素出现
await page.waitForSelector('button:has-text("保存")', { timeout: 10000 })
await page.click('button:has-text("保存")')
```

#### 2. 元素被遮挡

```
Error: Element is not visible
```

**解决方案**：
- 滚动到元素
- 关闭遮罩层
- 使用 `force: true`

```typescript
await page.locator('button').scrollIntoViewIfNeeded()
await page.keyboard.press('Escape') // 关闭遮罩
await page.click('button', { force: true })
```

#### 3. 认证失败

```
Error: 401 Unauthorized
```

**解决方案**：
- 使用 `authenticatedPage` fixture
- 检查 token 是否过期
- 使用 `autoFixPage` 自动处理登录

```typescript
import { authenticatedPage } from '../fixtures/auth.fixture'

test.use({ storageState: authenticatedPage })
```

#### 4. 网络超时

```
Error: page.goto: Timeout 30000ms exceeded
```

**解决方案**：
- 增加超时时间
- 检查后端服务是否运行
- 使用 mock API

```typescript
await page.goto('/admin/dashboard', { timeout: 60000 })
```

### 调试清单

- [ ] 后端服务是否运行？
- [ ] 数据库是否可访问？
- [ ] 选择器是否正确？
- [ ] 是否需要登录？
- [ ] 网络请求是否成功？
- [ ] 是否有 JavaScript 错误？
- [ ] 元素是否可见？
- [ ] 是否有遮罩层？

### 获取帮助

1. 查看 Playwright 文档：https://playwright.dev
2. 查看测试日志：`test-results/`
3. 查看自动修复报告：`test-results/autofix-report.md`
4. 使用 `--debug` 模式运行测试
5. 查看 trace：`npx playwright show-trace`

## 附录

### 配置文件

- `playwright.config.ts` - Playwright 配置
- `e2e/global-setup.ts` - 全局设置
- `e2e/global-teardown.ts` - 全局清理

### 相关文档

- [Playwright 官方文档](https://playwright.dev)
- [测试最佳实践](https://playwright.dev/docs/best-practices)
- [Page Object Model](https://playwright.dev/docs/pom)
- [CI/CD 集成](https://playwright.dev/docs/ci)
