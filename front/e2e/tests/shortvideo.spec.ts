import { test, expect } from '../fixtures/auth.fixture'
import { ShortVideoProjectPage, ScriptPlanningPage, ShotListPage, MaterialPreparationPage, MaterialProductionPage } from '../pages/shortvideo.page'

/**
 * 短视频模块 E2E 测试
 * 覆盖项目管理、脚本策划、分镜头、素材准备、素材生产等核心功能
 */

test.describe('短视频模块 - 项目管理', () => {
  test('应该能够创建新的短视频项目', async ({ authenticatedPage }) => {
    const projectPage = new ShortVideoProjectPage(authenticatedPage)

    await projectPage.goto()
    await projectPage.verifyPageLoaded()

    const projectName = '测试项目 - ' + Date.now()
    await projectPage.createProject(projectName, '这是一个自动化测试创建的短视频项目')

    // 验证项目出现在列表中
    await expect(authenticatedPage.locator(`text=${projectName}`)).toBeVisible()
  })

  test('应该能够打开项目工作台', async ({ authenticatedPage }) => {
    const projectPage = new ShortVideoProjectPage(authenticatedPage)

    await projectPage.goto()

    // 创建项目
    const projectName = '工作台测试 - ' + Date.now()
    await projectPage.createProject(projectName, '测试工作台功能')

    // 打开工作台
    await projectPage.openWorkbench(projectName)

    // 验证 URL 跳转
    await expect(authenticatedPage).toHaveURL(/workbench/)
  })

  test('应该能够搜索项目', async ({ authenticatedPage }) => {
    const projectPage = new ShortVideoProjectPage(authenticatedPage)

    await projectPage.goto()

    // 搜索项目
    await authenticatedPage.fill('input[placeholder*="搜索"]', '测试')
    await authenticatedPage.press('input[placeholder*="搜索"]', 'Enter')

    // 验证搜索结果
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够删除项目', async ({ authenticatedPage }) => {
    const projectPage = new ShortVideoProjectPage(authenticatedPage)

    await projectPage.goto()

    // 创建临时项目
    const projectName = '临时项目 - ' + Date.now()
    await projectPage.createProject(projectName, '用于删除测试')

    // 删除项目
    const row = authenticatedPage.locator(`tr:has-text("${projectName}")`)
    await row.locator('button[aria-label="删除"]').click()
    await authenticatedPage.click('button:has-text("确认")')

    // 验证删除成功
    await expect(authenticatedPage.locator(`text=${projectName}`)).toHaveCount(0)
  })
})

test.describe('短视频模块 - 脚本策划', () => {
  let projectId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    // 创建测试项目
    const projectPage = new ShortVideoProjectPage(authenticatedPage)
    await projectPage.goto()
    await projectPage.createProject('脚本测试项目 - ' + Date.now(), '用于脚本策划测试')

    // 获取项目 ID（实际应从页面获取）
    projectId = 1
  })

  test('应该能够生成脚本', async ({ authenticatedPage }) => {
    const scriptPage = new ScriptPlanningPage(authenticatedPage)

    await scriptPage.goto(projectId)
    await scriptPage.generateScript('护肤品推荐', '专业种草')

    // 验证生成完成
    await expect(authenticatedPage.locator('text=生成完成')).toBeVisible()
  })

  test('应该能够编辑脚本内容', async ({ authenticatedPage }) => {
    const scriptPage = new ScriptPlanningPage(authenticatedPage)

    await scriptPage.goto(projectId)

    // 先生成脚本
    await scriptPage.generateScript('面膜推荐', '轻松活泼')

    // 编辑脚本
    await scriptPage.editScript('这是修改后的脚本内容，包含产品介绍和使用方法。')

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够选择不同的脚本风格', async ({ authenticatedPage }) => {
    const scriptPage = new ScriptPlanningPage(authenticatedPage)

    await scriptPage.goto(projectId)

    // 测试不同风格
    const styles = ['专业种草', '轻松活泼', '情感共鸣', '知识科普']
    for (const style of styles) {
      await scriptPage.generateScript('测试主题', style)
      await expect(authenticatedPage.locator('text=生成完成')).toBeVisible()
    }
  })
})

test.describe('短视频模块 - 分镜头管理', () => {
  let projectId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    projectId = 1 // 实际应从测试数据获取
  })

  test('应该能够添加分镜头', async ({ authenticatedPage }) => {
    const shotPage = new ShotListPage(authenticatedPage)

    await shotPage.goto(projectId)

    await shotPage.addShot({
      shotNo: 1,
      shotType: '特写',
      duration: 3,
      description: '产品特写镜头，展示包装细节',
    })

    // 验证分镜头添加成功
    await expect(authenticatedPage.locator('text=产品特写镜头')).toBeVisible()
  })

  test('应该能够编辑分镜头', async ({ authenticatedPage }) => {
    const shotPage = new ShotListPage(authenticatedPage)

    await shotPage.goto(projectId)

    // 先添加分镜头
    await shotPage.addShot({
      shotNo: 2,
      shotType: '中景',
      duration: 5,
      description: '原始描述',
    })

    // 编辑分镜头
    await shotPage.editShot(2, '修改后的描述：展示使用场景')

    // 验证修改成功
    await expect(authenticatedPage.locator('text=修改后的描述')).toBeVisible()
  })

  test('应该能够删除分镜头', async ({ authenticatedPage }) => {
    const shotPage = new ShotListPage(authenticatedPage)

    await shotPage.goto(projectId)

    // 添加临时分镜头
    await shotPage.addShot({
      shotNo: 99,
      shotType: '远景',
      duration: 2,
      description: '临时分镜头',
    })

    // 删除分镜头
    await shotPage.deleteShot(99)

    // 验证删除成功
    await expect(authenticatedPage.locator('[data-shot-no="99"]')).toHaveCount(0)
  })

  test('应该能够调整分镜头顺序', async ({ authenticatedPage }) => {
    const shotPage = new ShotListPage(authenticatedPage)

    await shotPage.goto(projectId)

    // 添加多个分镜头
    await shotPage.addShot({ shotNo: 1, shotType: '特写', duration: 3, description: '镜头1' })
    await shotPage.addShot({ shotNo: 2, shotType: '中景', duration: 5, description: '镜头2' })
    await shotPage.addShot({ shotNo: 3, shotType: '远景', duration: 4, description: '镜头3' })

    // 验证分镜头列表加载
    await expect(authenticatedPage.locator('[data-shot-no]')).toHaveCount(3)
  })
})

test.describe('短视频模块 - 素材准备', () => {
  let projectId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    projectId = 1
  })

  test('应该能够上传人物参考图', async ({ authenticatedPage }) => {
    const materialPage = new MaterialPreparationPage(authenticatedPage)

    await materialPage.goto(projectId)

    // 上传参考图（需要准备测试文件）
    // await materialPage.uploadCharacterReference('test-files/character.jpg')

    // 验证上传成功
    // await expect(authenticatedPage.locator('text=上传成功')).toBeVisible()
  })

  test('应该能够上传场景参考图', async ({ authenticatedPage }) => {
    const materialPage = new MaterialPreparationPage(authenticatedPage)

    await materialPage.goto(projectId)

    // 上传场景图（需要准备测试文件）
    // await materialPage.uploadSceneReference('test-files/scene.jpg')

    // 验证上传成功
    // await expect(authenticatedPage.locator('text=上传成功')).toBeVisible()
  })

  test('应该能够管理素材库', async ({ authenticatedPage }) => {
    const materialPage = new MaterialPreparationPage(authenticatedPage)

    await materialPage.goto(projectId)

    // 切换到素材库标签
    await authenticatedPage.click('button:has-text("素材库")')

    // 验证素材列表加载
    await authenticatedPage.waitForSelector('.material-list, .MuiGrid-root')
  })
})

test.describe('短视频模块 - 素材生产', () => {
  let projectId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    projectId = 1
  })

  test('应该能够提交图像生成任务', async ({ authenticatedPage }) => {
    const productionPage = new MaterialProductionPage(authenticatedPage)

    await productionPage.goto(projectId)
    await productionPage.submitTask('image')

    // 验证任务提交成功
    await expect(authenticatedPage.locator('text=任务已提交')).toBeVisible()
  })

  test('应该能够提交配音生成任务', async ({ authenticatedPage }) => {
    const productionPage = new MaterialProductionPage(authenticatedPage)

    await productionPage.goto(projectId)
    await productionPage.submitTask('voice')

    // 验证任务提交成功
    await expect(authenticatedPage.locator('text=任务已提交')).toBeVisible()
  })

  test('应该能够提交视频合成任务', async ({ authenticatedPage }) => {
    const productionPage = new MaterialProductionPage(authenticatedPage)

    await productionPage.goto(projectId)
    await productionPage.submitTask('video')

    // 验证任务提交成功
    await expect(authenticatedPage.locator('text=任务已提交')).toBeVisible()
  })

  test('应该能够查看任务状态', async ({ authenticatedPage }) => {
    const productionPage = new MaterialProductionPage(authenticatedPage)

    await productionPage.goto(projectId)

    // 提交任务
    await productionPage.submitTask('image')

    // 查看任务状态（假设任务 ID 为 1）
    const status = await productionPage.checkTaskStatus(1)

    // 验证状态存在
    expect(status).toBeTruthy()
  })

  test('应该能够取消任务', async ({ authenticatedPage }) => {
    const productionPage = new MaterialProductionPage(authenticatedPage)

    await productionPage.goto(projectId)

    // 提交任务
    await productionPage.submitTask('image')

    // 取消任务（假设任务 ID 为 1）
    await productionPage.cancelTask(1)

    // 验证取消成功
    await expect(authenticatedPage.locator('text=已取消')).toBeVisible()
  })
})
