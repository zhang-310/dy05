import { test, expect } from '@playwright/test'

/**
 * 快速验证测试 - 验证 E2E 基础设施修复
 */

test.describe('快速验证', () => {
  test('应该能够访问登录页面', async ({ page }) => {
    console.log('🔍 测试开始：访问登录页面')

    await page.goto('/login')
    await page.waitForLoadState('domcontentloaded')

    console.log('✅ 页面加载完成')

    // 验证页面标题或登录表单存在
    const hasLoginForm = await page.locator('input[name="username"], input[type="text"]').count() > 0
    expect(hasLoginForm).toBeTruthy()

    console.log('✅ 登录表单存在')
  })

  test('应该能够检查后端健康状态', async ({ request }) => {
    console.log('🔍 测试开始：检查后端健康')

    const response = await request.get('http://localhost:8080/actuator/health')

    console.log(`📊 后端响应状态: ${response.status()}`)

    expect(response.ok()).toBeTruthy()

    const body = await response.json()
    console.log(`📊 后端健康状态: ${JSON.stringify(body)}`)

    expect(body.status).toBe('UP')

    console.log('✅ 后端服务健康')
  })

  test('应该能够访问前端首页', async ({ page }) => {
    console.log('🔍 测试开始：访问前端首页')

    await page.goto('/')
    await page.waitForLoadState('domcontentloaded')

    console.log('✅ 首页加载完成')

    // 验证页面加载成功（不是 404）
    const title = await page.title()
    console.log(`📊 页面标题: ${title}`)

    expect(title).not.toBe('')

    console.log('✅ 首页访问成功')
  })
})
