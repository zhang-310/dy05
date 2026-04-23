import { test, expect } from '../fixtures/auth.fixture'
import { CopyLibraryPage, CopyTemplatePage, CopyGenerationPage, CopyAnalysisPage } from '../pages/copy.page'

/**
 * 文案模块 E2E 测试
 * 覆盖文案库、模板、生成、分析等核心功能
 */

test.describe('文案模块 - 文案库管理', () => {
  test('应该能够创建新文案', async ({ authenticatedPage }) => {
    const copyPage = new CopyLibraryPage(authenticatedPage)

    await copyPage.goto()
    await copyPage.verifyPageLoaded()

    const copyTitle = '测试文案 - ' + Date.now()
    await copyPage.createCopy({
      title: copyTitle,
      content: '这是一段精心设计的营销文案，旨在吸引用户关注并促进转化。',
      category: '营销文案',
    })

    // 验证文案出现在列表中
    await expect(authenticatedPage.locator(`text=${copyTitle}`)).toBeVisible()
  })

  test('应该能够搜索文案', async ({ authenticatedPage }) => {
    const copyPage = new CopyLibraryPage(authenticatedPage)

    await copyPage.goto()
    await copyPage.searchCopy('营销')

    // 验证搜索结果
    await expect(authenticatedPage.locator('.MuiDataGrid-row')).not.toHaveCount(0)
  })

  test('应该能够编辑文案', async ({ authenticatedPage }) => {
    const copyPage = new CopyLibraryPage(authenticatedPage)

    await copyPage.goto()

    // 创建测试文案
    const copyTitle = '编辑测试文案 - ' + Date.now()
    await copyPage.createCopy({
      title: copyTitle,
      content: '原始文案内容',
      category: '产品文案',
    })

    // 编辑文案
    await copyPage.editCopy(copyTitle, '这是修改后的文案内容，更加精炼有力。')

    // 验证修改成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够删除文案', async ({ authenticatedPage }) => {
    const copyPage = new CopyLibraryPage(authenticatedPage)

    await copyPage.goto()

    // 创建临时文案
    const copyTitle = '临时文案 - ' + Date.now()
    await copyPage.createCopy({
      title: copyTitle,
      content: '临时内容',
      category: '活动文案',
    })

    // 删除文案
    await copyPage.deleteCopy(copyTitle)

    // 验证删除成功
    await expect(authenticatedPage.locator(`text=${copyTitle}`)).toHaveCount(0)
  })

  test('应该能够复制文案到剪贴板', async ({ authenticatedPage }) => {
    const copyPage = new CopyLibraryPage(authenticatedPage)

    await copyPage.goto()

    // 创建文案
    const copyTitle = '复制测试文案 - ' + Date.now()
    await copyPage.createCopy({
      title: copyTitle,
      content: '这是要复制的文案内容',
      category: '营销文案',
    })

    // 复制文案
    await copyPage.copyCopyToClipboard(copyTitle)

    // 验证复制成功
    await expect(authenticatedPage.locator('text=复制成功')).toBeVisible()
  })

  test('应该能够按类目筛选文案', async ({ authenticatedPage }) => {
    const copyPage = new CopyLibraryPage(authenticatedPage)

    await copyPage.goto()

    // 按类目筛选
    await copyPage.filterByCategory('产品文案')

    // 验证筛选结果加载
    await authenticatedPage.waitForLoadState('networkidle')
  })
})

test.describe('文案模块 - 文案模板', () => {
  test('应该能够创建文案模板', async ({ authenticatedPage }) => {
    const templatePage = new CopyTemplatePage(authenticatedPage)

    await templatePage.goto()
    await templatePage.verifyPageLoaded()

    const templateName = '测试模板 - ' + Date.now()
    await templatePage.createTemplate({
      name: templateName,
      content: '{{品牌名}}的{{产品名}}，{{卖点1}}，{{卖点2}}，现在购买立享{{优惠}}！',
      variables: ['品牌名', '产品名', '卖点1', '卖点2', '优惠'],
    })

    // 验证模板创建成功
    await expect(authenticatedPage.locator(`text=${templateName}`)).toBeVisible()
  })

  test('应该能够使用文案模板', async ({ authenticatedPage }) => {
    const templatePage = new CopyTemplatePage(authenticatedPage)

    await templatePage.goto()

    // 创建模板
    const templateName = '使用测试模板 - ' + Date.now()
    await templatePage.createTemplate({
      name: templateName,
      content: '{{产品}}，{{特点}}',
      variables: ['产品', '特点'],
    })

    // 使用模板
    await templatePage.useTemplate(templateName, {
      产品: '补水面膜',
      特点: '深层补水，持久保湿',
    })

    // 验证生成结果显示
    await expect(authenticatedPage.locator('.generated-copy')).toBeVisible()
  })

  test('应该能够编辑文案模板', async ({ authenticatedPage }) => {
    const templatePage = new CopyTemplatePage(authenticatedPage)

    await templatePage.goto()

    // 创建模板
    const templateName = '编辑测试模板 - ' + Date.now()
    await templatePage.createTemplate({
      name: templateName,
      content: '原始模板内容',
      variables: ['变量1'],
    })

    // 编辑模板
    await templatePage.editTemplate(templateName, '修改后的模板内容 {{变量1}} {{变量2}}')

    // 验证修改成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够删除文案模板', async ({ authenticatedPage }) => {
    const templatePage = new CopyTemplatePage(authenticatedPage)

    await templatePage.goto()

    // 创建临时模板
    const templateName = '临时模板 - ' + Date.now()
    await templatePage.createTemplate({
      name: templateName,
      content: '临时内容',
      variables: [],
    })

    // 删除模板
    await templatePage.deleteTemplate(templateName)

    // 验证删除成功
    await expect(authenticatedPage.locator(`text=${templateName}`)).toHaveCount(0)
  })
})

test.describe('文案模块 - 文案生成', () => {
  test('应该能够生成文案', async ({ authenticatedPage }) => {
    const genPage = new CopyGenerationPage(authenticatedPage)

    await genPage.goto()
    await genPage.verifyPageLoaded()

    await genPage.generateCopy({
      theme: '护肤品促销',
      style: '专业可信',
      length: 100,
    })

    // 验证生成完成
    await expect(authenticatedPage.locator('text=生成完成')).toBeVisible()
  })

  test('应该能够保存生成的文案', async ({ authenticatedPage }) => {
    const genPage = new CopyGenerationPage(authenticatedPage)

    await genPage.goto()

    // 生成文案
    await genPage.generateCopy({
      theme: '新品上市',
      style: '活泼有趣',
      length: 150,
    })

    // 保存文案
    const copyTitle = '生成的文案 - ' + Date.now()
    await genPage.saveGeneratedCopy(copyTitle)

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够重新生成文案', async ({ authenticatedPage }) => {
    const genPage = new CopyGenerationPage(authenticatedPage)

    await genPage.goto()

    // 首次生成
    await genPage.generateCopy({
      theme: '限时优惠',
      style: '紧迫感',
      length: 80,
    })

    // 重新生成
    await genPage.regenerate()

    // 验证重新生成完成
    await expect(authenticatedPage.locator('text=生成完成')).toBeVisible()
  })

  test('应该能够调整文案语气', async ({ authenticatedPage }) => {
    const genPage = new CopyGenerationPage(authenticatedPage)

    await genPage.goto()

    // 生成文案
    await genPage.generateCopy({
      theme: '会员福利',
      style: '温馨亲切',
      length: 120,
    })

    // 调整语气
    await genPage.adjustTone('正式专业')

    // 验证调整完成
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够选择不同的文案风格', async ({ authenticatedPage }) => {
    const genPage = new CopyGenerationPage(authenticatedPage)

    await genPage.goto()

    const styles = ['专业可信', '活泼有趣', '温馨亲切', '紧迫感']
    for (const style of styles) {
      await genPage.generateCopy({
        theme: '产品推广',
        style: style,
        length: 100,
      })

      await expect(authenticatedPage.locator('text=生成完成')).toBeVisible()
    }
  })
})

test.describe('文案模块 - 文案分析', () => {
  test('应该能够分析文案', async ({ authenticatedPage }) => {
    const analysisPage = new CopyAnalysisPage(authenticatedPage)

    await analysisPage.goto()
    await analysisPage.verifyPageLoaded()

    await analysisPage.analyzeCopy(
      '这款面膜采用天然植物精华，深层补水，让您的肌肤水润透亮。现在购买立享8折优惠！'
    )

    // 验证分析结果显示
    await expect(authenticatedPage.locator('.analysis-result')).toBeVisible()
  })

  test('应该能够查看情感分析', async ({ authenticatedPage }) => {
    const analysisPage = new CopyAnalysisPage(authenticatedPage)

    await analysisPage.goto()

    // 分析文案
    await analysisPage.analyzeCopy(
      '限时抢购！超值优惠！不容错过的好机会！'
    )

    // 查看情感分析
    await analysisPage.viewEmotionAnalysis()

    // 验证情感图表显示
    await expect(authenticatedPage.locator('.emotion-chart')).toBeVisible()
  })

  test('应该能够查看关键词分析', async ({ authenticatedPage }) => {
    const analysisPage = new CopyAnalysisPage(authenticatedPage)

    await analysisPage.goto()

    // 分析文案
    await analysisPage.analyzeCopy(
      '专业护肤，科学配方，温和不刺激，适合敏感肌肤使用。'
    )

    // 查看关键词分析
    await analysisPage.viewKeywordAnalysis()

    // 验证关键词列表显示
    await expect(authenticatedPage.locator('.keyword-list')).toBeVisible()
  })

  test('应该能够导出分析报告', async ({ authenticatedPage }) => {
    const analysisPage = new CopyAnalysisPage(authenticatedPage)

    await analysisPage.goto()

    // 分析文案
    await analysisPage.analyzeCopy(
      '新品上市，品质保证，值得信赖。'
    )

    // 导出报告
    await analysisPage.exportAnalysisReport()

    // 验证导出成功
    await expect(authenticatedPage.locator('text=导出成功')).toBeVisible()
  })

  test('应该能够分析不同类型的文案', async ({ authenticatedPage }) => {
    const analysisPage = new CopyAnalysisPage(authenticatedPage)

    await analysisPage.goto()

    const copies = [
      '专业护肤，科学配方',
      '限时优惠，立即抢购',
      '温馨提示，关爱您的肌肤',
      '新品上市，敬请期待',
    ]

    for (const copy of copies) {
      await analysisPage.analyzeCopy(copy)
      await expect(authenticatedPage.locator('.analysis-result')).toBeVisible()
    }
  })
})
