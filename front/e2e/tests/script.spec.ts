import { test, expect } from '../fixtures/auth.fixture'
import { ScriptListPage, ScriptGenerationPage, ScriptOptimizationPage, ScriptCompliancePage, ScriptTemplatePage } from '../pages/script.page'

/**
 * 话术模块 E2E 测试
 * 覆盖话术管理、生成、优化、合规检测、模板等核心功能
 */

test.describe('话术模块 - 话术管理', () => {
  test('应该能够创建新话术', async ({ authenticatedPage }) => {
    const scriptPage = new ScriptListPage(authenticatedPage)

    await scriptPage.goto()
    await scriptPage.verifyPageLoaded()

    const scriptTitle = '测试话术 - ' + Date.now()
    await scriptPage.createScript({
      title: scriptTitle,
      content: '大家好，欢迎来到我的直播间！今天给大家带来一款超级好用的护肤品。',
      type: '开场话术',
    })

    // 验证话术出现在列表中
    await expect(authenticatedPage.locator(`text=${scriptTitle}`)).toBeVisible()
  })

  test('应该能够搜索话术', async ({ authenticatedPage }) => {
    const scriptPage = new ScriptListPage(authenticatedPage)

    await scriptPage.goto()
    await scriptPage.searchScript('护肤')

    // 验证搜索结果
    await expect(authenticatedPage.locator('.MuiDataGrid-row')).not.toHaveCount(0)
  })

  test('应该能够编辑话术', async ({ authenticatedPage }) => {
    const scriptPage = new ScriptListPage(authenticatedPage)

    await scriptPage.goto()

    // 创建测试话术
    const scriptTitle = '编辑测试话术 - ' + Date.now()
    await scriptPage.createScript({
      title: scriptTitle,
      content: '原始内容',
      type: '产品介绍',
    })

    // 编辑话术
    await scriptPage.editScript(scriptTitle, '这是修改后的话术内容，更加生动有趣。')

    // 验证修改成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够删除话术', async ({ authenticatedPage }) => {
    const scriptPage = new ScriptListPage(authenticatedPage)

    await scriptPage.goto()

    // 创建临时话术
    const scriptTitle = '临时话术 - ' + Date.now()
    await scriptPage.createScript({
      title: scriptTitle,
      content: '临时内容',
      type: '结束话术',
    })

    // 删除话术
    await scriptPage.deleteScript(scriptTitle)

    // 验证删除成功
    await expect(authenticatedPage.locator(`text=${scriptTitle}`)).toHaveCount(0)
  })

  test('应该能够复制话术', async ({ authenticatedPage }) => {
    const scriptPage = new ScriptListPage(authenticatedPage)

    await scriptPage.goto()

    // 创建话术
    const scriptTitle = '复制测试话术 - ' + Date.now()
    await scriptPage.createScript({
      title: scriptTitle,
      content: '这是要复制的话术内容',
      type: '产品介绍',
    })

    // 复制话术
    await scriptPage.copyScript(scriptTitle)

    // 验证复制成功
    await expect(authenticatedPage.locator('text=复制成功')).toBeVisible()
  })
})

test.describe('话术模块 - 话术生成', () => {
  test('应该能够生成话术', async ({ authenticatedPage }) => {
    const genPage = new ScriptGenerationPage(authenticatedPage)

    await genPage.goto()
    await genPage.verifyPageLoaded()

    await genPage.generateScript({
      productName: '补水面膜',
      style: '专业种草',
      duration: 60,
    })

    // 验证生成完成
    await expect(authenticatedPage.locator('text=生成完成')).toBeVisible()
  })

  test('应该能够保存生成的话术', async ({ authenticatedPage }) => {
    const genPage = new ScriptGenerationPage(authenticatedPage)

    await genPage.goto()

    // 生成话术
    await genPage.generateScript({
      productName: '精华液',
      style: '轻松活泼',
      duration: 90,
    })

    // 保存话术
    const scriptTitle = '生成的话术 - ' + Date.now()
    await genPage.saveGeneratedScript(scriptTitle)

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够重新生成话术', async ({ authenticatedPage }) => {
    const genPage = new ScriptGenerationPage(authenticatedPage)

    await genPage.goto()

    // 首次生成
    await genPage.generateScript({
      productName: '口红',
      style: '情感共鸣',
      duration: 45,
    })

    // 重新生成
    await genPage.regenerate()

    // 验证重新生成完成
    await expect(authenticatedPage.locator('text=生成完成')).toBeVisible()
  })

  test('应该能够选择不同的话术风格', async ({ authenticatedPage }) => {
    const genPage = new ScriptGenerationPage(authenticatedPage)

    await genPage.goto()

    const styles = ['专业种草', '轻松活泼', '情感共鸣', '知识科普']
    for (const style of styles) {
      await genPage.generateScript({
        productName: '眼霜',
        style: style,
        duration: 60,
      })

      await expect(authenticatedPage.locator('text=生成完成')).toBeVisible()
    }
  })
})

test.describe('话术模块 - 话术优化', () => {
  test('应该能够优化话术', async ({ authenticatedPage }) => {
    const optPage = new ScriptOptimizationPage(authenticatedPage)

    await optPage.goto()
    await optPage.verifyPageLoaded()

    await optPage.optimizeScript(
      '这个产品很好用，大家快来买吧。',
      '增强吸引力'
    )

    // 验证优化完成
    await expect(authenticatedPage.locator('text=优化完成')).toBeVisible()
  })

  test('应该能够对比优化前后版本', async ({ authenticatedPage }) => {
    const optPage = new ScriptOptimizationPage(authenticatedPage)

    await optPage.goto()

    // 优化话术
    await optPage.optimizeScript(
      '产品介绍内容',
      '提升专业度'
    )

    // 对比版本
    await optPage.compareVersions()

    // 验证对比界面显示
    await expect(authenticatedPage.locator('.version-comparison')).toBeVisible()
  })

  test('应该能够应用优化结果', async ({ authenticatedPage }) => {
    const optPage = new ScriptOptimizationPage(authenticatedPage)

    await optPage.goto()

    // 优化话术
    await optPage.optimizeScript(
      '简单的产品描述',
      '增加细节'
    )

    // 应用优化
    await optPage.applyOptimization()

    // 验证应用成功
    await expect(authenticatedPage.locator('text=应用成功')).toBeVisible()
  })

  test('应该能够选择不同的优化类型', async ({ authenticatedPage }) => {
    const optPage = new ScriptOptimizationPage(authenticatedPage)

    await optPage.goto()

    const types = ['增强吸引力', '提升专业度', '增加细节', '简化表达']
    for (const type of types) {
      await optPage.optimizeScript('测试话术内容', type)
      await expect(authenticatedPage.locator('text=优化完成')).toBeVisible()
    }
  })
})

test.describe('话术模块 - 合规检测', () => {
  test('应该能够检测话术合规性', async ({ authenticatedPage }) => {
    const compliancePage = new ScriptCompliancePage(authenticatedPage)

    await compliancePage.goto()
    await compliancePage.verifyPageLoaded()

    await compliancePage.checkCompliance(
      '这款产品能够治疗所有皮肤问题，效果立竿见影！'
    )

    // 验证检测结果显示
    await expect(authenticatedPage.locator('.compliance-result')).toBeVisible()
  })

  test('应该能够查看违规项', async ({ authenticatedPage }) => {
    const compliancePage = new ScriptCompliancePage(authenticatedPage)

    await compliancePage.goto()

    // 检测包含违规内容的话术
    await compliancePage.checkCompliance(
      '绝对有效，包治百病，国家认证！'
    )

    // 查看违规项
    await compliancePage.viewViolations()

    // 验证违规列表显示
    await expect(authenticatedPage.locator('.violation-list')).toBeVisible()
  })

  test('应该能够修复违规项', async ({ authenticatedPage }) => {
    const compliancePage = new ScriptCompliancePage(authenticatedPage)

    await compliancePage.goto()

    // 检测话术
    await compliancePage.checkCompliance(
      '最好的产品，绝对有效！'
    )

    // 查看违规项
    await compliancePage.viewViolations()

    // 修复第一个违规项
    await compliancePage.fixViolation(1)

    // 验证修复成功
    await expect(authenticatedPage.locator('text=修复成功')).toBeVisible()
  })

  test('应该能够检测合规的话术', async ({ authenticatedPage }) => {
    const compliancePage = new ScriptCompliancePage(authenticatedPage)

    await compliancePage.goto()

    await compliancePage.checkCompliance(
      '这款护肤品质地轻盈，适合日常使用，帮助改善肌肤状态。'
    )

    // 验证检测通过
    await expect(authenticatedPage.locator('.compliance-result')).toContainText('通过')
  })
})

test.describe('话术模块 - 话术模板', () => {
  test('应该能够创建话术模板', async ({ authenticatedPage }) => {
    const templatePage = new ScriptTemplatePage(authenticatedPage)

    await templatePage.goto()
    await templatePage.verifyPageLoaded()

    const templateName = '测试模板 - ' + Date.now()
    await templatePage.createTemplate({
      name: templateName,
      content: '大家好，我是{{主播名}}，今天给大家推荐{{产品名}}。',
      category: '开场模板',
    })

    // 验证模板创建成功
    await expect(authenticatedPage.locator(`text=${templateName}`)).toBeVisible()
  })

  test('应该能够使用话术模板', async ({ authenticatedPage }) => {
    const templatePage = new ScriptTemplatePage(authenticatedPage)

    await templatePage.goto()

    // 创建模板
    const templateName = '使用测试模板 - ' + Date.now()
    await templatePage.createTemplate({
      name: templateName,
      content: '模板内容',
      category: '产品介绍',
    })

    // 使用模板
    await templatePage.useTemplate(templateName)

    // 验证跳转到生成页面
    await expect(authenticatedPage).toHaveURL(/script\/generate/)
  })

  test('应该能够编辑话术模板', async ({ authenticatedPage }) => {
    const templatePage = new ScriptTemplatePage(authenticatedPage)

    await templatePage.goto()

    // 创建模板
    const templateName = '编辑测试模板 - ' + Date.now()
    await templatePage.createTemplate({
      name: templateName,
      content: '原始模板内容',
      category: '结束模板',
    })

    // 编辑模板
    await templatePage.editTemplate(templateName, '修改后的模板内容')

    // 验证修改成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够删除话术模板', async ({ authenticatedPage }) => {
    const templatePage = new ScriptTemplatePage(authenticatedPage)

    await templatePage.goto()

    // 创建临时模板
    const templateName = '临时模板 - ' + Date.now()
    await templatePage.createTemplate({
      name: templateName,
      content: '临时内容',
      category: '通用模板',
    })

    // 删除模板
    await templatePage.deleteTemplate(templateName)

    // 验证删除成功
    await expect(authenticatedPage.locator(`text=${templateName}`)).toHaveCount(0)
  })
})
