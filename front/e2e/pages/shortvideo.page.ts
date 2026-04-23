import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, fillForm, expectToast, selectOption } from '../utils/test-helpers'

/**
 * Page Object Model - 短视频模块页面
 */

export class ShortVideoProjectPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/shortvideo/projects')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '短视频项目')
    await expectTableLoaded(this.page)
  }

  async createProject(name: string, description: string) {
    await this.page.click('button:has-text("新建项目")')
    await fillForm(this.page, { projectName: name, description })
    await this.page.click('button:has-text("创建")')
    await expectToast(this.page, '创建成功')
  }

  async openWorkbench(projectName: string) {
    const row = this.page.locator(`tr:has-text("${projectName}")`)
    await row.locator('button:has-text("工作台")').click()
    await this.page.waitForURL(/workbench/)
  }
}

export class ScriptPlanningPage {
  constructor(private page: Page) {}

  async goto(projectId: number) {
    await this.page.goto(`/admin/shortvideo/script-planning?projectId=${projectId}`)
    await waitForPageLoad(this.page)
  }

  async generateScript(theme: string, style: string) {
    await this.page.fill('input[name="theme"]', theme)
    await selectOption(this.page, '风格', style)
    await this.page.click('button:has-text("生成脚本")')
    await this.page.waitForSelector('text=生成完成', { timeout: 30000 })
  }

  async editScript(content: string) {
    await this.page.fill('textarea[name="scriptContent"]', content)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }
}

export class ShotListPage {
  constructor(private page: Page) {}

  async goto(projectId: number) {
    await this.page.goto(`/admin/shortvideo/shot-list?projectId=${projectId}`)
    await waitForPageLoad(this.page)
  }

  async addShot(shotData: { shotNo: number; shotType: string; duration: number; description: string }) {
    await this.page.click('button:has-text("新增分镜")')
    await fillForm(this.page, {
      shotNo: String(shotData.shotNo),
      duration: String(shotData.duration),
      description: shotData.description,
    })
    await selectOption(this.page, '镜头类型', shotData.shotType)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async editShot(shotNo: number, newDescription: string) {
    const shot = this.page.locator(`[data-shot-no="${shotNo}"]`)
    await shot.locator('button:has-text("编辑")').click()
    await this.page.fill('textarea[name="description"]', newDescription)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async deleteShot(shotNo: number) {
    const shot = this.page.locator(`[data-shot-no="${shotNo}"]`)
    await shot.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }
}

export class MaterialPreparationPage {
  constructor(private page: Page) {}

  async goto(projectId: number) {
    await this.page.goto(`/admin/shortvideo/material-prepare?projectId=${projectId}`)
    await waitForPageLoad(this.page)
  }

  async uploadCharacterReference(filePath: string) {
    await this.page.setInputFiles('input[type="file"][accept="image/*"]', filePath)
    await this.page.waitForSelector('text=上传成功')
  }

  async uploadSceneReference(filePath: string) {
    const inputs = await this.page.locator('input[type="file"][accept="image/*"]').all()
    await inputs[1].setInputFiles(filePath)
    await this.page.waitForSelector('text=上传成功')
  }
}

export class MaterialProductionPage {
  constructor(private page: Page) {}

  async goto(projectId: number) {
    await this.page.goto(`/admin/shortvideo/material-production?projectId=${projectId}`)
    await waitForPageLoad(this.page)
  }

  async submitTask(taskType: 'image' | 'voice' | 'video') {
    await selectOption(this.page, '生产类型', taskType === 'image' ? '关键帧图像' : taskType === 'voice' ? '配音生成' : '视频合成')
    await this.page.click('button:has-text("提交任务")')
    await expectToast(this.page, '任务已提交')
  }

  async checkTaskStatus(taskId: number) {
    const task = this.page.locator(`[data-task-id="${taskId}"]`)
    return await task.locator('.status').textContent()
  }

  async cancelTask(taskId: number) {
    const task = this.page.locator(`[data-task-id="${taskId}"]`)
    await task.locator('button:has-text("取消")').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '已取消')
  }
}
