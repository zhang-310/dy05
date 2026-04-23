import { Page } from '@playwright/test'
import { BasePage } from './BasePage'

/**
 * 登录页面对象
 */
export class LoginPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await super.goto('/login')
  }

  async login(username: string, password: string) {
    await this.fillInput('username', username)
    await this.fillInput('password', password)
    await this.clickButton('登录')
    await this.page.waitForURL(/\/admin/, { timeout: 10000 })
  }

  async expectLoginError(message: string) {
    const error = this.page.locator('.error-message, .MuiAlert-root')
    await error.waitFor({ state: 'visible' })
    return error.filter({ hasText: message })
  }
}

/**
 * 直播场次列表页面对象
 */
export class LiveSessionsPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await super.goto('/admin/live/sessions')
  }

  async createSession(title: string, description: string) {
    await this.openDialog('新建场次')
    await this.fillInput('liveTitle', title)
    await this.fillInput('liveDescription', description)
    await this.clickButton('保存')
    await this.expectToast('保存成功')
  }

  async editSession(sessionTitle: string, newTitle: string) {
    await this.clickRowAction(sessionTitle, '编辑')
    await this.page.waitForSelector('.MuiDialog-root')
    await this.fillInput('liveTitle', newTitle)
    await this.clickButton('保存')
    await this.expectToast('保存成功')
  }

  async deleteSession(sessionTitle: string) {
    await this.clickRowAction(sessionTitle, '删除')
    await this.page.click('button:has-text("确定")')
    await this.expectToast('删除成功')
  }

  async searchSession(keyword: string) {
    await this.search(keyword)
  }
}

/**
 * 话术生成页面对象
 */
export class ScriptGeneratePage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await super.goto('/admin/script/generate')
  }

  async generateScript(productName: string, style: string, duration: string) {
    await this.fillInput('productName', productName)
    await this.page.click(`label:has-text("${style}")`)
    await this.fillInput('duration', duration)
    await this.clickButton('生成话术')
    await this.page.waitForSelector('text=生成完成', { timeout: 60000 })
  }

  async getGeneratedScript() {
    const scriptContent = this.page.locator('.generated-script, textarea[name="scriptContent"]')
    return await scriptContent.textContent()
  }

  async saveScript(scriptName: string) {
    await this.fillInput('scriptName', scriptName)
    await this.clickButton('保存')
    await this.expectToast('保存成功')
  }
}

/**
 * 商品列表页面对象
 */
export class ProductListPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await super.goto('/admin/product/list')
  }

  async createProduct(name: string, price: string) {
    await this.openDialog('新建商品')
    await this.fillInput('productName', name)
    await this.fillInput('price', price)
    await this.clickButton('保存')
    await this.expectToast('保存成功')
  }

  async editProduct(productName: string, newPrice: string) {
    await this.clickRowAction(productName, '编辑')
    await this.fillInput('price', newPrice)
    await this.clickButton('保存')
    await this.expectToast('保存成功')
  }

  async deleteProduct(productName: string) {
    await this.clickRowAction(productName, '删除')
    await this.page.click('button:has-text("确定")')
    await this.expectToast('删除成功')
  }

  async batchExport(count: number) {
    await this.selectRows(count)
    await this.clickButton('批量操作')
    await this.clickButton('导出')
    await this.expectToast('导出成功')
  }
}

/**
 * 知识库页面对象
 */
export class KnowledgeBasePage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await super.goto('/admin/ai/knowledge-base')
  }

  async createKnowledgeBase(name: string, description: string) {
    await this.openDialog('新建知识库')
    await this.fillInput('name', name)
    await this.fillInput('description', description)
    await this.clickButton('创建')
    await this.expectToast('创建成功')
  }

  async uploadDocument(kbName: string, filePath: string) {
    await this.clickRowAction(kbName, '上传文档')
    const fileInput = this.page.locator('input[type="file"]')
    await fileInput.setInputFiles(filePath)
    await this.page.waitForTimeout(1000)
    await this.clickButton('确定')
    await this.expectToast('上传成功')
  }

  async deleteKnowledgeBase(kbName: string) {
    await this.clickRowAction(kbName, '删除')
    await this.page.click('button:has-text("确定")')
    await this.expectToast('删除成功')
  }
}

/**
 * 智能体列表页面对象
 */
export class AgentListPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await super.goto('/admin/agent/list')
  }

  async createAgent(name: string, description: string, type: string) {
    await this.openDialog('新建智能体')
    await this.fillInput('agentName', name)
    await this.fillInput('agentDescription', description)
    await this.selectOption('智能体类型', type)
    await this.fillInput('systemPrompt', '你是一个专业的助手')
    await this.clickButton('创建')
    await this.expectToast('创建成功')
  }

  async chatWithAgent(agentName: string, message: string) {
    await this.clickRowAction(agentName, '对话')
    await this.page.waitForURL(/\/admin\/agent\/chat/)
    const input = this.page.locator('textarea[placeholder*="输入消息"]')
    await input.fill(message)
    await this.page.click('button[aria-label="发送"], button:has-text("发送")')
    await this.page.waitForSelector('.message-response', { timeout: 30000 })
  }

  async deleteAgent(agentName: string) {
    await this.clickRowAction(agentName, '删除')
    await this.page.click('button:has-text("确定")')
    await this.expectToast('删除成功')
  }
}

/**
 * 用户管理页面对象
 */
export class UserManagementPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await super.goto('/admin/system/users')
  }

  async createUser(username: string, email: string, role: string) {
    await this.openDialog('新建用户')
    await this.fillInput('username', username)
    await this.fillInput('email', email)
    await this.fillInput('password', 'Test123456')
    await this.selectOption('角色', role)
    await this.clickButton('创建')
    await this.expectToast('创建成功')
  }

  async editUser(username: string, newEmail: string) {
    await this.clickRowAction(username, '编辑')
    await this.fillInput('email', newEmail)
    await this.clickButton('保存')
    await this.expectToast('保存成功')
  }

  async disableUser(username: string) {
    await this.clickRowAction(username, '禁用')
    await this.page.click('button:has-text("确定")')
    await this.expectToast('操作成功')
  }

  async deleteUser(username: string) {
    await this.clickRowAction(username, '删除')
    await this.page.click('button:has-text("确定")')
    await this.expectToast('删除成功')
  }
}
