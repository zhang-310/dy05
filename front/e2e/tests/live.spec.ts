import { test, expect } from '../fixtures/auth.fixture'
import { LiveSessionListPage, LiveScriptPage, LiveProductPage } from '../pages/live.page'

/**
 * Live 模块 E2E 测试
 * 覆盖直播场次、话术管理、商品管理等核心功能
 */

test.describe('Live 模块 - 直播场次管理', () => {
  test('应该能够创建新的直播场次', async ({ authenticatedPage }) => {
    const livePage = new LiveSessionListPage(authenticatedPage)

    await livePage.goto()
    await livePage.verifyPageLoaded()

    await livePage.clickCreateButton()
    await livePage.fillSessionForm({
      title: '测试直播场次 - ' + Date.now(),
      description: '这是一个自动化测试创建的直播场次',
    })
    await livePage.submitForm()

    // 验证场次出现在列表中
    await expect(authenticatedPage.locator('text=测试直播场次')).toBeVisible()
  })

  test('应该能够搜索直播场次', async ({ authenticatedPage }) => {
    const livePage = new LiveSessionListPage(authenticatedPage)

    await livePage.goto()
    await livePage.searchSession('测试')

    // 验证搜索结果
    await expect(authenticatedPage.locator('.MuiDataGrid-row')).not.toHaveCount(0)
  })

  test('应该能够删除直播场次', async ({ authenticatedPage }) => {
    const livePage = new LiveSessionListPage(authenticatedPage)

    await livePage.goto()

    // 创建一个临时场次用于删除
    await livePage.clickCreateButton()
    const tempTitle = '临时场次 - ' + Date.now()
    await livePage.fillSessionForm({ title: tempTitle })
    await livePage.submitForm()

    // 删除该场次
    await livePage.deleteSession(tempTitle)

    // 验证场次已被删除
    await expect(authenticatedPage.locator(`text=${tempTitle}`)).toHaveCount(0)
  })
})

test.describe('Live 模块 - 话术管理', () => {
  let sessionId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    // 创建测试用直播场次
    const livePage = new LiveSessionListPage(authenticatedPage)
    await livePage.goto()
    await livePage.clickCreateButton()
    await livePage.fillSessionForm({ title: '话术测试场次 - ' + Date.now() })
    await livePage.submitForm()

    // 获取场次 ID（从 URL 或数据属性）
    sessionId = 1 // 实际应从页面获取
  })

  test('应该能够添加开场话术', async ({ authenticatedPage }) => {
    const scriptPage = new LiveScriptPage(authenticatedPage)

    await scriptPage.goto(sessionId)
    await scriptPage.verifyPageLoaded()

    await scriptPage.addScript('开场', '大家好，欢迎来到我的直播间！')

    // 验证话术已添加
    await expect(authenticatedPage.locator('text=大家好，欢迎来到我的直播间！')).toBeVisible()
  })

  test('应该能够编辑话术内容', async ({ authenticatedPage }) => {
    const scriptPage = new LiveScriptPage(authenticatedPage)

    await scriptPage.goto(sessionId)

    // 先添加一条话术
    await scriptPage.addScript('产品', '这款产品非常好用')

    // 编辑话术
    await scriptPage.editScript(1, '这款产品性价比超高，强烈推荐！')

    // 验证话术已更新
    await expect(authenticatedPage.locator('text=这款产品性价比超高，强烈推荐！')).toBeVisible()
  })

  test('应该能够使用 AI 生成话术', async ({ authenticatedPage }) => {
    const scriptPage = new LiveScriptPage(authenticatedPage)

    await scriptPage.goto(sessionId)
    await scriptPage.generateScript('面膜')

    // 验证生成完成
    await expect(authenticatedPage.locator('text=生成完成')).toBeVisible()
    await expect(authenticatedPage.locator('.generated-script')).toBeVisible()
  })
})

test.describe('Live 模块 - 商品管理', () => {
  let sessionId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    sessionId = 1 // 实际应从测试数据获取
  })

  test('应该能够添加商品到直播场次', async ({ authenticatedPage }) => {
    const productPage = new LiveProductPage(authenticatedPage)

    await productPage.goto(sessionId)
    await productPage.addProduct('测试商品')

    // 验证商品已添加
    await expect(authenticatedPage.locator('text=测试商品')).toBeVisible()
  })

  test('应该能够设置商品类型', async ({ authenticatedPage }) => {
    const productPage = new LiveProductPage(authenticatedPage)

    await productPage.goto(sessionId)
    await productPage.setProductType('测试商品', '引流款')

    // 验证类型已设置
    await expect(authenticatedPage.locator('text=引流款')).toBeVisible()
  })

  test('应该能够移除商品', async ({ authenticatedPage }) => {
    const productPage = new LiveProductPage(authenticatedPage)

    await productPage.goto(sessionId)
    await productPage.addProduct('临时商品')
    await productPage.removeProduct('临时商品')

    // 验证商品已移除
    await expect(authenticatedPage.locator('text=临时商品')).toHaveCount(0)
  })
})
