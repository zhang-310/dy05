import { test as base, expect } from '@playwright/test'

const test = base

/**
 * 认证模块 E2E 测试
 * 覆盖登录、注册、密码重置等核心功能
 */

test.describe('认证模块 - 登录', () => {
  test('应该能够成功登录', async ({ page }) => {
    await page.goto('/login')

    // 填写登录表单 - 使用 label 定位 MUI TextField
    await page.getByLabel('用户名').fill('admin')
    await page.getByLabel('密码').fill('admin123')
    await page.click('button:has-text("登录")')

    // 验证跳转到首页
    await page.waitForURL('/admin/dashboard', { timeout: 10000 })
    await expect(page).toHaveURL(/\/admin\/dashboard/)
  })

  test('应该显示错误信息当用户名或密码错误', async ({ page }) => {
    await page.goto('/login')

    // 填写错误的登录信息
    await page.getByLabel('用户名').fill('wronguser')
    await page.getByLabel('密码').fill('wrongpass')
    await page.click('button:has-text("登录")')

    // 验证错误提示
    await expect(page.locator('text=用户名或密码错误')).toBeVisible({ timeout: 5000 })
  })

  test('应该验证必填字段', async ({ page }) => {
    await page.goto('/login')

    // 不填写任何信息直接点击登录
    await page.click('button:has-text("登录")')

    // 验证表单验证提示
    await expect(page.locator('text=请输入用户名和密码')).toBeVisible({ timeout: 5000 })
  })

  test.skip('应该能够记住登录状态', async ({ page }) => {
    // Skip: 登录页面没有"记住我"功能
    await page.goto('/login')

    // 登录
    await page.getByLabel('用户名').fill('admin')
    await page.getByLabel('密码').fill('admin123')
    await page.click('button:has-text("登录")')

    // 验证登录成功
    await page.waitForURL('/admin/dashboard')

    // 刷新页面，验证仍然保持登录状态
    await page.reload()
    await expect(page).toHaveURL(/\/admin\/dashboard/)
  })
})

test.describe.skip('认证模块 - 注册', () => {
  // Skip: 注册功能未实现
  test('应该能够注册新用户', async ({ page }) => {
    await page.goto('/register')

    const username = 'newuser_' + Date.now()
    const email = `${username}@example.com`

    // 填写注册表单
    await page.getByLabel('用户名').fill(username)
    await page.getByLabel('邮箱').fill(email)
    await page.getByLabel('密码').fill('Password123!')
    await page.getByLabel('确认密码').fill('Password123!')
    await page.click('button:has-text("注册")')

    // 验证注册成功提示
    await expect(page.locator('text=注册成功')).toBeVisible()
  })

  test('应该验证密码强度', async ({ page }) => {
    await page.goto('/register')

    // 填写弱密码
    await page.getByLabel('用户名').fill('testuser')
    await page.getByLabel('邮箱').fill('test@example.com')
    await page.getByLabel('密码').fill('123')
    await page.getByLabel('确认密码').fill('123')
    await page.click('button:has-text("注册")')

    // 验证密码强度提示
    await expect(page.locator('text=密码强度不足')).toBeVisible()
  })

  test('应该验证两次密码是否一致', async ({ page }) => {
    await page.goto('/register')

    // 填写不一致的密码
    await page.getByLabel('用户名').fill('testuser')
    await page.getByLabel('邮箱').fill('test@example.com')
    await page.getByLabel('密码').fill('Password123!')
    await page.getByLabel('确认密码').fill('Password456!')
    await page.click('button:has-text("注册")')

    // 验证密码不一致提示
    await expect(page.locator('text=两次密码不一致')).toBeVisible()
  })

  test('应该验证邮箱格式', async ({ page }) => {
    await page.goto('/register')

    // 填写无效的邮箱
    await page.getByLabel('用户名').fill('testuser')
    await page.getByLabel('邮箱').fill('invalid-email')
    await page.getByLabel('密码').fill('Password123!')
    await page.getByLabel('确认密码').fill('Password123!')
    await page.click('button:has-text("注册")')

    // 验证邮箱格式提示
    await expect(page.locator('text=邮箱格式不正确')).toBeVisible()
  })
})

test.describe.skip('认证模块 - 密码重置', () => {
  // Skip: 密码重置功能未实现
  test('应该能够请求重置密码', async ({ page }) => {
    await page.goto('/forgot-password')

    // 填写邮箱
    await page.getByLabel('邮箱').fill('admin@example.com')
    await page.click('button:has-text("发送重置链接")')

    // 验证发送成功提示
    await expect(page.locator('text=重置链接已发送')).toBeVisible()
  })

  test('应该验证邮箱是否存在', async ({ page }) => {
    await page.goto('/forgot-password')

    // 填写不存在的邮箱
    await page.getByLabel('邮箱').fill('nonexistent@example.com')
    await page.click('button:has-text("发送重置链接")')

    // 验证邮箱不存在提示
    await expect(page.locator('text=该邮箱未注册')).toBeVisible()
  })

  test('应该能够重置密码', async ({ page }) => {
    // 假设已经有重置令牌
    await page.goto('/reset-password?token=test-token')

    // 填写新密码
    await page.getByLabel('新密码').fill('NewPassword123!')
    await page.getByLabel('确认密码').fill('NewPassword123!')
    await page.click('button:has-text("重置密码")')

    // 验证重置成功提示
    await expect(page.locator('text=密码重置成功')).toBeVisible()
  })
})

test.describe.skip('认证模块 - 登出', () => {
  // Skip: 需要先实现登录后的用户菜单
  test('应该能够登出', async ({ page }) => {
    // 先登录
    await page.goto('/login')
    await page.getByLabel('用户名').fill('admin')
    await page.getByLabel('密码').fill('admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('/admin/dashboard')

    // 登出
    await page.click('button[aria-label="用户菜单"]')
    await page.click('button:has-text("退出登录")')

    // 验证跳转到登录页
    await expect(page).toHaveURL(/\/login/)
  })

  test('登出后应该清除认证信息', async ({ page }) => {
    // 先登录
    await page.goto('/login')
    await page.getByLabel('用户名').fill('admin')
    await page.getByLabel('密码').fill('admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('/admin/dashboard')

    // 登出
    await page.click('button[aria-label="用户菜单"]')
    await page.click('button:has-text("退出登录")')

    // 尝试访问受保护的页面
    await page.goto('/admin/dashboard')

    // 验证被重定向到登录页
    await expect(page).toHaveURL(/\/login/)
  })
})

test.describe('认证模块 - 权限控制', () => {
  test('未登录用户应该被重定向到登录页', async ({ page }) => {
    await page.goto('/admin/dashboard')

    // 验证被重定向到登录页
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 })
  })

  test.skip('应该能够访问公开页面', async ({ page }) => {
    // Skip: /about 页面未实现
    await page.goto('/about')

    // 验证可以访问公开页面
    await expect(page).toHaveURL(/\/about/)
  })

  test('登录后应该能够访问受保护的页面', async ({ page }) => {
    // 先登录
    await page.goto('/login')
    await page.getByLabel('用户名').fill('admin')
    await page.getByLabel('密码').fill('admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('/admin/dashboard', { timeout: 10000 })

    // 访问受保护的页面
    await page.goto('/admin/product/list')

    // 验证可以访问
    await expect(page).toHaveURL(/\/admin\/product\/list/, { timeout: 10000 })
  })
})
