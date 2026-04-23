/**
 * W-10 测试框架 - E2E 测试样板（Playwright/Cypress）
 */

/// <reference types="playwright" />

import { test, expect } from '@playwright/test';

/**
 * E2E 测试样板
 *
 * 工具：Playwright（跨浏览器支持）
 * 测试覆盖：
 * - 用户登录和权限验证
 * - 核心业务流程（创建 → 生成 → 搜索 → 发版）
 * - 搜索和结果展示
 * - 支付流程
 * - 监控告警
 * - 响应式设计
 */

import { Page, Browser, BrowserContext, Route } from '@playwright/test';

test.describe('DY01 E2E 测试套件', () => {
  let page: Page;

  test.beforeEach(async ({ browser }: { browser: Browser }) => {
    // 创建新的浏览器上下文和页面
    const context: BrowserContext = await browser.newContext();
    page = await context.newPage();

    // 设置视口大小
    await page.setViewportSize({ width: 1280, height: 720 });

    // 基础 URL
    test.use({ baseURL: 'http://localhost:3000' });
  });

  /**
   * 示例 1：用户登录和权限验证
   */
  test('用户应该能够成功登录并访问受保护页面', async () => {
    // 访问登录页面
    await page.goto('/auth/login');
    await expect(page).toHaveTitle(/登录/);

    // 填写登录表单
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'testpass123');

    // 提交登录
    await page.click('button:has-text("登录")');

    // 等待重定向到仪表板
    await page.waitForURL('/dashboard');
    await expect(page.locator('.page-title')).toContainText('仪表板');
  });

  /**
   * 示例 2：话术版本管理流程
   */
  test('用户应该能够创建和管理话术版本', async () => {
    // 登录
    await loginUser(page);

    // 导航到话术库
    await page.click('a:has-text("话术库")');
    await page.waitForURL('/live/script-library');

    // 创建新话术
    await page.click('button:has-text("新增话术")');
    await page.fill('textarea[name="content"]', '测试话术内容');
    await page.click('button:has-text("保存")');

    // 验证话术创建成功
    await expect(page.locator('text=保存成功')).toBeVisible();

    // 版本管理
    await page.click('.version-selector');
    await expect(page.locator('.version-list')).toContainText('v1');
    await expect(page.locator('.version-list')).toContainText('v2');
  });

  /**
   * 示例 3：AI 批量生成流程
   */
  test('用户应该能够提交 AI 生成任务', async () => {
    await loginUser(page);

    // 导航到 AI 生成页面
    await page.click('a:has-text("AI 生成")');
    await page.waitForURL('/ai/generation');

    // 输入原始内容
    await page.fill('textarea[name="content"]', '原始话术内容');

    // 选择生成风格
    await page.click('[data-style="professional"]');
    await page.click('[data-style="passionate"]');

    // 设置优先级
    await page.click('.priority-selector');
    await page.click('text=高');

    // 提交生成任务
    await page.click('button:has-text("提交生成")');

    // 验证提交成功和进度展示
    await expect(page.locator('text=已提交')).toBeVisible();
    await expect(page.locator('.progress-item')).toBeVisible();
  });

  /**
   * 示例 4：混合搜索功能测试
   */
  test('用户应该能够使用混合搜索找到内容', async () => {
    await loginUser(page);

    // 导航到搜索页面
    await page.click('a:has-text("搜索")');
    await page.waitForURL('/ai/search');

    // 输入搜索词
    await page.fill('input[placeholder*="搜索"]', '热销商品');

    // 验证搜索建议出现
    await expect(page.locator('.search-suggestions')).toBeVisible();

    // 提交搜索
    await page.press('input[placeholder*="搜索"]', 'Enter');

    // 验证搜索结果
    await page.waitForURL(/.*search.*keyword/);
    await expect(page.locator('.search-result')).toBeTruthy();

    // 验证评分展示（RRF 融合分数）
    await expect(page.locator('.final-score')).toContainText('%');
  });

  /**
   * 示例 5：实时监控仪表板
   */
  test('用户应该能够查看实时监控数据', async () => {
    await loginUser(page);

    // 导航到监控仪表板
    await page.click('a:has-text("监控")');
    await page.waitForURL('/monitoring/dashboard');

    // 验证实时指标面板
    await expect(page.locator('text=API 平均延迟')).toBeVisible();
    await expect(page.locator('text=CPU 使用率')).toBeVisible();
    await expect(page.locator('text=缓存命中率')).toBeVisible();

    // 验证告警显示
    await expect(page.locator('.alerts-panel')).toBeVisible();

    // 检查系统健康状态
    const healthStatus = await page.locator('.health-status');
    const status = await healthStatus.textContent();
    expect(['健康', '警告', '严重']).toContain(status);
  });

  /**
   * 示例 6：支付流程测试
   */
  test('用户应该能够完成支付流程', async () => {
    await loginUser(page);

    // 导航到订单页面
    await page.goto('/orders');

    // 创建订单
    await page.click('button:has-text("购买配额")');
    await page.click('[data-plan="pro"]');

    // 选择支付方式
    await page.click('.payment-method');
    await page.click('text=抖音支付');

    // 确认订单
    await page.click('button:has-text("立即支付")');

    // 等待支付页面（模拟支付完成）
    await page.waitForURL(/payment|alipay|wechat/);

    // 模拟支付成功回调
    await page.goto('/payment/callback?orderId=123&status=success');

    // 验证订单状态
    await expect(page.locator('text=支付成功')).toBeVisible();
  });

  /**
   * 示例 7：响应式设计测试
   */
  test('页面在移动设备上应该正常显示', async () => {
    // 设置移动视口
    await page.setViewportSize({ width: 375, height: 812 }); // iPhone

    await loginUser(page);

    // 验证菜单折叠
    const menuButton = page.locator('[data-testid="menu-toggle"]');
    await expect(menuButton).toBeVisible();

    // 点击菜单
    await menuButton.click();
    await expect(page.locator('.mobile-menu')).toBeVisible();

    // 验证内容可读性
    const content = page.locator('main');
    const boundingBox = await content.boundingBox();
    expect(boundingBox?.width).toBeLessThanOrEqual(375);
  });

  /**
   * 示例 8：性能测试
   */
  test('页面加载性能应该在目标范围内', async () => {
    await loginUser(page);

    // 测量首屏加载时间
    const navigationTiming = await page.evaluate(() => {
      const timing = window.performance.timing;
      return timing.loadEventEnd - timing.navigationStart;
    });

    expect(navigationTiming).toBeLessThan(2000); // < 2 秒

    // 测量 API 响应时间
    await page.goto('/ai/search');
    await page.fill('input[placeholder*="搜索"]', '测试');

    const apiResponseTime = await page.evaluate(async () => {
      const start = performance.now();
      await fetch('/api/v1/ai/search/suggestions?query=test');
      const end = performance.now();
      return end - start;
    });

    expect(apiResponseTime).toBeLessThan(500); // < 500ms
  });

  /**
   * 示例 9：无障碍访问性测试
   */
  test('页面应该满足无障碍访问要求', async () => {
    await loginUser(page);

    // 验证按钮有文本标签
    const buttons = await page.locator('button').all();
    for (const button of buttons) {
      const text = await button.textContent();
      expect(text?.trim().length).toBeGreaterThan(0);
    }

    // 验证表单有标签
    const inputs = await page.locator('input').all();
    for (const input of inputs) {
      const ariaLabel = await input.getAttribute('aria-label');
      const label = input.locator('.. label');
      expect(ariaLabel || await label.isVisible()).toBeTruthy();
    }

    // 验证图像有 alt 文本
    const images = await page.locator('img').all();
    for (const image of images) {
      const alt = await image.getAttribute('alt');
      expect(alt).toBeTruthy();
    }
  });

  /**
   * 示例 10：错误处理和重试
   */
  test('页面应该正确处理网络错误', async () => {
    await loginUser(page);

    // 模拟网络错误
    await page.route('/api/v1/**', (route: Route) => {
      route.abort('failed');
    });

    // 尝试加载数据
    await page.goto('/dashboard');

    // 验证错误信息或重试按钮
    const errorMessage = page.locator('text=加载失败');
    const retryButton = page.locator('button:has-text("重试")');

    const hasError = await errorMessage.isVisible();
    const hasRetry = await retryButton.isVisible();

    expect(hasError || hasRetry).toBeTruthy();
  });
});

/**
 * 辅助函数：登录用户
 */
async function loginUser(page: Page): Promise<void> {
  await page.goto('/auth/login');
  await page.fill('input[name="username"]', 'testuser');
  await page.fill('input[name="password"]', 'testpass123');
  await page.click('button:has-text("登录")');
  await page.waitForURL('/dashboard');
}
