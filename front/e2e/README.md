# E2E 测试文档

本文档描述了 dy02 项目的端到端（E2E）测试架构和使用方法。

## 🎉 新功能：自动后端服务管理

**更新日期**: 2026-04-07

E2E 测试现在支持自动启动和停止后端服务！无需手动启动 Spring Boot 应用，测试框架会自动处理。

### 主要特性

- ✅ **自动检测**: 检查后端服务是否已运行
- ✅ **自动启动**: 如果未运行，自动启动 Spring Boot（测试模式）
- ✅ **健康检查**: 等待后端健康检查通过（最多 2 分钟）
- ✅ **自动停止**: 测试完成后自动停止后端服务
- ✅ **环境变量**: 支持通过环境变量配置行为

### 快速开始

```bash
# 一键运行 E2E 测试（自动启动后端）
npm run test:e2e
```

就这么简单！测试框架会自动：
1. 检查后端是否运行
2. 如果未运行，启动 Spring Boot（测试模式）
3. 等待健康检查通过
4. 运行所有测试
5. 测试完成后停止后端

### 环境变量配置

#### E2E_AUTO_START_BACKEND
控制是否自动启动后端服务（默认：`true`）

```bash
# 自动启动后端（默认）
npm run test:e2e

# 不自动启动后端（需要手动启动）
E2E_AUTO_START_BACKEND=false npm run test:e2e
```

#### BACKEND_URL
指定后端服务地址（默认：`http://localhost:8080`）

```bash
# 使用自定义地址
BACKEND_URL=http://localhost:9090 npm run test:e2e
```

#### TEST_USERNAME / TEST_PASSWORD
测试用户凭证（默认：`admin` / `admin123`）

```bash
# 使用自定义凭证
TEST_USERNAME=testuser TEST_PASSWORD=testpass npm run test:e2e
```

### 手动启动模式

如果你想手动管理后端服务：

```bash
# 1. 启动后端（测试模式）
cd C:\claude\dy02
mvn spring-boot:run -Dspring-boot.run.profiles=test

# 2. 运行 E2E 测试（不自动启动后端）
cd front
E2E_AUTO_START_BACKEND=false npm run test:e2e
```

### 故障排查

#### 后端启动失败

**检查清单**:
- ✓ Maven 已安装：`mvn --version`
- ✓ Java 17+ 已安装：`java -version`
- ✓ 端口 8080 未被占用：`netstat -ano | findstr :8080`
- ✓ 数据库服务运行中（PostgreSQL）
- ✓ Redis 服务运行中
- ✓ RabbitMQ 服务运行中

#### 健康检查超时

如果等待超过 2 分钟：
1. 手动访问 http://localhost:8080/actuator/health
2. 检查后端日志输出
3. 确认所有依赖服务（数据库、Redis、RabbitMQ）正常运行

---

## 测试框架

- **Playwright**: 现代化的端到端测试框架
- **TypeScript**: 类型安全的测试代码
- **多浏览器支持**: Chromium, Firefox, WebKit, Mobile Chrome, Mobile Safari
- **自动后端管理**: 自动启动/停止 Spring Boot 服务

## 目录结构

```
front/e2e/
├── fixtures/
│   └── auth.fixture.ts          # 认证 fixture（自动登录）
├── pages/                        # Page Object Model
│   ├── ai.page.ts               # AI 模块页面对象
│   ├── agent.page.ts            # 智能体模块页面对象
│   ├── copy.page.ts             # 文案模块页面对象
│   ├── dashboard.page.ts        # Dashboard 模块页面对象
│   ├── douyin.page.ts           # 抖音模块页面对象
│   ├── live.page.ts             # 直播模块页面对象
│   ├── product.page.ts          # 商品模块页面对象
│   ├── script.page.ts           # 话术模块页面对象
│   ├── shortvideo.page.ts       # 短视频模块页面对象
│   └── system.page.ts           # 系统模块页面对象
├── tests/                        # 测试用例
│   ├── ai.spec.ts               # AI 模块测试
│   ├── agent.spec.ts            # 智能体模块测试
│   ├── auth.spec.ts             # 认证模块测试
│   ├── copy.spec.ts             # 文案模块测试
│   ├── dashboard.spec.ts        # Dashboard 模块测试
│   ├── douyin.spec.ts           # 抖音模块测试
│   ├── live.spec.ts             # 直播模块测试
│   ├── product.spec.ts          # 商品模块测试
│   ├── script.spec.ts           # 话术模块测试
│   ├── shortvideo.spec.ts       # 短视频模块测试
│   └── system.spec.ts           # 系统模块测试
└── utils/
    └── test-helpers.ts          # 测试辅助函数
```

## 运行测试

### 安装依赖

```bash
cd front
npm install
```

### 运行所有测试

```bash
# 无头模式运行所有测试
npm run test:e2e

# 或使用 Playwright 命令
npx playwright test
```

### 运行特定测试

```bash
# 运行单个测试文件
npx playwright test e2e/tests/live.spec.ts

# 运行特定测试用例
npx playwright test e2e/tests/live.spec.ts -g "应该能够创建新的直播场次"

# 运行特定浏览器
npx playwright test --project=chromium
```

### 调试模式

```bash
# UI 模式（推荐）
npx playwright test --ui

# 调试模式
npx playwright test --debug

# 查看测试报告
npx playwright show-report
```

### 生成测试代码

```bash
# 使用 Playwright Codegen 录制测试
npx playwright codegen http://localhost:3000
```

## 测试覆盖范围

### 已完成的测试模块（11 个）

1. **认证模块** (`auth.spec.ts`)
   - 登录/登出
   - 注册
   - 密码重置
   - 权限控制

2. **直播模块** (`live.spec.ts`)
   - 直播场次管理
   - 话术管理
   - 商品管理

3. **AI 模块** (`ai.spec.ts`)
   - 知识库管理
   - Prompt 模板管理
   - 知识自进化

4. **短视频模块** (`shortvideo.spec.ts`)
   - 项目管理
   - 脚本策划
   - 分镜头管理
   - 素材准备
   - 素材生产

5. **商品模块** (`product.spec.ts`)
   - 商品管理
   - 商品详情
   - 类目管理
   - 效果评分

6. **话术模块** (`script.spec.ts`)
   - 话术管理
   - 话术生成
   - 话术优化
   - 合规检测
   - 话术模板

7. **文案模块** (`copy.spec.ts`)
   - 文案库管理
   - 文案模板
   - 文案生成
   - 文案分析

8. **智能体模块** (`agent.spec.ts`)
   - 智能体管理
   - 智能体对话
   - 智能体配置
   - 智能体分析

9. **系统模块** (`system.spec.ts`)
   - 用户管理
   - 角色管理
   - 系统配置
   - 审计日志
   - 系统监控
   - 告警管理

10. **抖音模块** (`douyin.spec.ts`)
    - 账号管理
    - 数据同步
    - 数据分析
    - 直播管理

11. **Dashboard 及其他** (`dashboard.spec.ts`)
    - 管理驾驶舱
    - 报表中心
    - 支付管理
    - A/B 测试
    - 工作流

### 测试统计

- **测试文件**: 11 个
- **Page Object 文件**: 10 个
- **测试用例**: 150+ 个
- **覆盖页面**: 128 个页面的核心功能

## Page Object Model (POM)

所有测试使用 Page Object Model 模式，将页面操作封装为可复用的方法。

### 示例

```typescript
// Page Object
export class LiveSessionListPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/live/sessions')
    await waitForPageLoad(this.page)
  }

  async createSession(data: { title: string; description: string }) {
    await this.page.click('button:has-text("新建场次")')
    await fillForm(this.page, {
      liveTitle: data.title,
      liveDescription: data.description,
    })
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }
}

// 测试用例
test('应该能够创建新的直播场次', async ({ authenticatedPage }) => {
  const livePage = new LiveSessionListPage(authenticatedPage)
  await livePage.goto()
  await livePage.createSession({
    title: '测试直播场次',
    description: '测试描述',
  })
  await expect(authenticatedPage.locator('text=测试直播场次')).toBeVisible()
})
```

## 认证 Fixture

所有需要登录的测试使用 `authenticatedPage` fixture，自动处理登录逻辑。

```typescript
import { test, expect } from '../fixtures/auth.fixture'

test('测试用例', async ({ authenticatedPage }) => {
  // authenticatedPage 已经登录，可以直接使用
  await authenticatedPage.goto('/admin/dashboard')
})
```

## 测试辅助函数

`test-helpers.ts` 提供了常用的测试辅助函数：

- `waitForPageLoad()` - 等待页面加载完成
- `waitForApiResponse()` - 等待 API 请求完成
- `fillForm()` - 填写表单字段
- `clickAndNavigate()` - 点击并等待导航
- `expectToast()` - 验证 Toast 消息
- `expectTableLoaded()` - 验证表格数据加载
- `expectDialogOpen()` - 验证对话框打开
- `selectOption()` - 选择下拉选项
- `expectPageTitle()` - 验证页面标题
- `mockApiResponse()` - 模拟 API 响应

## 最佳实践

### 1. 使用 Page Object Model

将页面操作封装为方法，提高代码复用性和可维护性。

### 2. 使用有意义的选择器

```typescript
// 好的做法
await page.click('button:has-text("保存")')
await page.locator('[data-testid="submit-button"]').click()

// 避免
await page.click('.MuiButton-root:nth-child(3)')
```

### 3. 等待元素可见

```typescript
// 等待元素出现
await expect(page.locator('text=保存成功')).toBeVisible()

// 等待网络空闲
await page.waitForLoadState('networkidle')
```

### 4. 使用独立的测试数据

每个测试应该使用独立的测试数据，避免测试之间的相互影响。

```typescript
const uniqueName = '测试数据 - ' + Date.now()
```

### 5. 清理测试数据

在测试结束后清理创建的测试数据。

```typescript
test.afterEach(async ({ authenticatedPage }) => {
  // 清理测试数据
})
```

### 6. 处理异步操作

对于 AI 生成等长时间操作，设置合理的超时时间。

```typescript
await page.waitForSelector('text=生成完成', { timeout: 30000 })
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
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Install dependencies
        run: |
          cd front
          npm ci
      - name: Install Playwright Browsers
        run: npx playwright install --with-deps
      - name: Run E2E tests
        run: npm run test:e2e
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: front/playwright-report/
```

## 故障排查

### 测试失败

1. 查看测试报告：`npx playwright show-report`
2. 查看截图和视频（在 `test-results/` 目录）
3. 使用 UI 模式调试：`npx playwright test --ui`

### 元素找不到

1. 检查选择器是否正确
2. 增加等待时间
3. 使用 Playwright Inspector：`npx playwright test --debug`

### 超时问题

1. 增加超时时间：`{ timeout: 60000 }`
2. 检查网络请求是否完成
3. 检查页面是否正确加载

## 未来改进

1. **增加视觉回归测试**: 使用 Playwright 的截图对比功能
2. **性能测试**: 测量页面加载时间和关键操作耗时
3. **可访问性测试**: 使用 axe-core 进行可访问性检查
4. **API 测试**: 直接测试后端 API
5. **测试数据管理**: 使用 fixtures 或数据库 seeding
6. **并行测试**: 优化测试执行时间
7. **测试覆盖率报告**: 生成详细的覆盖率报告

## 参考资料

- [Playwright 官方文档](https://playwright.dev/)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Page Object Model](https://playwright.dev/docs/pom)
