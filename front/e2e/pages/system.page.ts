import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, fillForm, expectToast, selectOption } from '../utils/test-helpers'

/**
 * Page Object Model - 系统模块页面
 */

export class UserManagementPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/auth/users')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '用户管理')
    await expectTableLoaded(this.page)
  }

  async createUser(data: { username: string; email: string; role: string }) {
    await this.page.click('button:has-text("新建用户")')
    await fillForm(this.page, {
      username: data.username,
      email: data.email,
    })
    await selectOption(this.page, '角色', data.role)
    await this.page.click('button:has-text("创建")')
    await expectToast(this.page, '创建成功')
  }

  async searchUser(keyword: string) {
    await this.page.fill('input[placeholder*="搜索"]', keyword)
    await this.page.press('input[placeholder*="搜索"]', 'Enter')
    await waitForPageLoad(this.page)
  }

  async editUser(username: string, newEmail: string) {
    const row = this.page.locator(`tr:has-text("${username}")`)
    await row.locator('button:has-text("编辑")').click()
    await this.page.fill('input[name="email"]', newEmail)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async disableUser(username: string) {
    const row = this.page.locator(`tr:has-text("${username}")`)
    await row.locator('button:has-text("禁用")').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '禁用成功')
  }

  async resetPassword(username: string) {
    const row = this.page.locator(`tr:has-text("${username}")`)
    await row.locator('button:has-text("重置密码")').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '重置成功')
  }
}

export class RoleManagementPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/auth/roles')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '角色管理')
    await expectTableLoaded(this.page)
  }

  async createRole(data: { name: string; description: string }) {
    await this.page.click('button:has-text("新建角色")')
    await fillForm(this.page, {
      roleName: data.name,
      roleDescription: data.description,
    })
    await this.page.click('button:has-text("创建")')
    await expectToast(this.page, '创建成功')
  }

  async assignPermissions(roleName: string, permissions: string[]) {
    const row = this.page.locator(`tr:has-text("${roleName}")`)
    await row.locator('button:has-text("权限")').click()

    for (const permission of permissions) {
      await this.page.click(`label:has-text("${permission}")`)
    }

    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async deleteRole(roleName: string) {
    const row = this.page.locator(`tr:has-text("${roleName}")`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }
}

export class SystemConfigPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/config')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '系统配置')
  }

  async updateConfig(key: string, value: string) {
    await this.page.fill(`input[name="${key}"]`, value)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async toggleFeature(featureName: string) {
    await this.page.click(`label:has-text("${featureName}")`)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async resetToDefault() {
    await this.page.click('button:has-text("恢复默认")')
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '恢复成功')
  }
}

export class AuditLogPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/log/audit')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '审计日志')
    await expectTableLoaded(this.page)
  }

  async filterByUser(username: string) {
    await this.page.fill('input[name="username"]', username)
    await this.page.click('button:has-text("查询")')
    await waitForPageLoad(this.page)
  }

  async filterByAction(action: string) {
    await selectOption(this.page, '操作类型', action)
    await this.page.click('button:has-text("查询")')
    await waitForPageLoad(this.page)
  }

  async filterByDateRange(startDate: string, endDate: string) {
    await this.page.fill('input[name="startDate"]', startDate)
    await this.page.fill('input[name="endDate"]', endDate)
    await this.page.click('button:has-text("查询")')
    await waitForPageLoad(this.page)
  }

  async viewLogDetail(logId: number) {
    await this.page.click(`[data-log-id="${logId}"]`)
    await this.page.waitForSelector('.log-detail')
  }

  async exportLogs() {
    await this.page.click('button:has-text("导出日志")')
    await expectToast(this.page, '导出成功')
  }
}

export class MonitoringPage {
  constructor(private page: Page)

  async goto() {
    await this.page.goto('/admin/system/monitoring')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '系统监控')
  }

  async viewSystemMetrics() {
    await this.page.click('button:has-text("系统指标")')
    await this.page.waitForSelector('.metrics-chart')
  }

  async viewApiMetrics() {
    await this.page.click('button:has-text("API 指标")')
    await this.page.waitForSelector('.api-metrics')
  }

  async viewDatabaseMetrics() {
    await this.page.click('button:has-text("数据库指标")')
    await this.page.waitForSelector('.db-metrics')
  }

  async refreshMetrics() {
    await this.page.click('button:has-text("刷新")')
    await waitForPageLoad(this.page)
  }

  async setAutoRefresh(interval: number) {
    await this.page.fill('input[name="refreshInterval"]', String(interval))
    await this.page.click('button:has-text("应用")')
  }
}

export class AlertManagementPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/system/alerts')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '告警管理')
    await expectTableLoaded(this.page)
  }

  async createAlert(data: { name: string; condition: string; threshold: number }) {
    await this.page.click('button:has-text("新建告警")')
    await fillForm(this.page, {
      alertName: data.name,
      threshold: String(data.threshold),
    })
    await selectOption(this.page, '告警条件', data.condition)
    await this.page.click('button:has-text("创建")')
    await expectToast(this.page, '创建成功')
  }

  async acknowledgeAlert(alertId: number) {
    const row = this.page.locator(`[data-alert-id="${alertId}"]`)
    await row.locator('button:has-text("确认")').click()
    await expectToast(this.page, '确认成功')
  }

  async resolveAlert(alertId: number) {
    const row = this.page.locator(`[data-alert-id="${alertId}"]`)
    await row.locator('button:has-text("解决")').click()
    await this.page.fill('textarea[name="resolution"]', '问题已解决')
    await this.page.click('button:has-text("提交")')
    await expectToast(this.page, '解决成功')
  }

  async deleteAlert(alertId: number) {
    const row = this.page.locator(`[data-alert-id="${alertId}"]`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }
}
