import { test, expect } from '../fixtures/auth.fixture'
import { SHORTVIDEO_SMOKE_ROUTE_PATHS } from '../../src/constants/shortvideoRoutes'

/**
 * 短视频已注册路由冒烟：确保 URL 与 router 一致且非 404（需已登录）。
 */
test.describe('短视频路由冒烟', () => {
  for (const path of SHORTVIDEO_SMOKE_ROUTE_PATHS) {
    test(`可打开 ${path}`, async ({ authenticatedPage }) => {
      const response = await authenticatedPage.goto(path, { waitUntil: 'domcontentloaded' })
      expect(response?.status(), `HTTP ${response?.status()} for ${path}`).not.toBe(404)
      await expect(authenticatedPage).not.toHaveURL(/\/login(\/|$)/)
      await expect(authenticatedPage.locator('body')).toBeVisible()
    })
  }
})
