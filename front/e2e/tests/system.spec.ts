import { test, expect } from '../fixtures/auth.fixture'
import { UserManagementPage, RoleManagementPage, SystemConfigPage, AuditLogPage, MonitoringPage, AlertManagementPage } from '../pages/system.page'

/**
 * 系统模块 E2E 测试
 * 覆盖用户管理、角色管理、系统配置、审计日志、监控、告警等核心功能
 */

test.describe('系统模块 - 用户管理', () => {
  test('应该能够创建新用户', async ({ authenticatedPage }) => {
    const userPage = new UserManagementPage(authenticatedPage)

    await userPage.goto()
    await userPage.verifyPageLoaded()

    const username = 'testuser_' + Date.now()
    await userPage.createUser({
      username: username,
      email: `${username}@example.com`,
      role: '普通用户',
    })

    // 验证用户出现在列表中
    await expect(authenticatedPage.locator(`text=${username}`)).toBeVisible()
  })

  test('应该能够搜索用户', async ({ authenticatedPage }) => {
    const userPage = new UserManagementPage(authenticatedPage)

    await userPage.goto()
    await userPage.searchUser('test')

    // 验证搜索结果
    await expect(authenticatedPage.locator('.MuiDataGrid-row')).not.toHaveCount(0)
  })

  test('应该能够编辑用户信息', async ({ authenticatedPage }) => {
    const userPage = new UserManagementPage(authenticatedPage)

    await userPage.goto()

    // 创建测试用户
    const username = 'edituser_' + Date.now()
    await userPage.createUser({
      username: username,
      email: `${username}@example.com`,
      role: '普通用户',
    })

    // 编辑用户
    await userPage.editUser(username, `new_${username}@example.com`)

    // 验证修改成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够禁用用户', async ({ authenticatedPage }) => {
    const userPage = new UserManagementPage(authenticatedPage)

    await userPage.goto()

    // 创建测试用户
    const username = 'disableuser_' + Date.now()
    await userPage.createUser({
      username: username,
      email: `${username}@example.com`,
      role: '普通用户',
    })

    // 禁用用户
    await userPage.disableUser(username)

    // 验证禁用成功
    await expect(authenticatedPage.locator('text=禁用成功')).toBeVisible()
  })

  test('应该能够重置用户密码', async ({ authenticatedPage }) => {
    const userPage = new UserManagementPage(authenticatedPage)

    await userPage.goto()

    // 创建测试用户
    const username = 'resetuser_' + Date.now()
    await userPage.createUser({
      username: username,
      email: `${username}@example.com`,
      role: '普通用户',
    })

    // 重置密码
    await userPage.resetPassword(username)

    // 验证重置成功
    await expect(authenticatedPage.locator('text=重置成功')).toBeVisible()
  })
})

test.describe('系统模块 - 角色管理', () => {
  test('应该能够创建新角色', async ({ authenticatedPage }) => {
    const rolePage = new RoleManagementPage(authenticatedPage)

    await rolePage.goto()
    await rolePage.verifyPageLoaded()

    const roleName = '测试角色_' + Date.now()
    await rolePage.createRole({
      name: roleName,
      description: '这是一个测试角色',
    })

    // 验证角色出现在列表中
    await expect(authenticatedPage.locator(`text=${roleName}`)).toBeVisible()
  })

  test('应该能够分配权限', async ({ authenticatedPage }) => {
    const rolePage = new RoleManagementPage(authenticatedPage)

    await rolePage.goto()

    // 创建测试角色
    const roleName = '权限测试角色_' + Date.now()
    await rolePage.createRole({
      name: roleName,
      description: '用于测试权限分配',
    })

    // 分配权限
    await rolePage.assignPermissions(roleName, ['查看商品', '编辑商品', '删除商品'])

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够删除角色', async ({ authenticatedPage }) => {
    const rolePage = new RoleManagementPage(authenticatedPage)

    await rolePage.goto()

    // 创建临时角色
    const roleName = '临时角色_' + Date.now()
    await rolePage.createRole({
      name: roleName,
      description: '临时角色',
    })

    // 删除角色
    await rolePage.deleteRole(roleName)

    // 验证删除成功
    await expect(authenticatedPage.locator(`text=${roleName}`)).toHaveCount(0)
  })
})

test.describe('系统模块 - 系统配置', () => {
  test('应该能够更新系统配置', async ({ authenticatedPage }) => {
    const configPage = new SystemConfigPage(authenticatedPage)

    await configPage.goto()
    await configPage.verifyPageLoaded()

    await configPage.updateConfig('systemName', '测试系统')

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够切换功能开关', async ({ authenticatedPage }) => {
    const configPage = new SystemConfigPage(authenticatedPage)

    await configPage.goto()

    await configPage.toggleFeature('启用邮件通知')

    // 验证保存成功
    await expect(authenticatedPage.locator('text=保存成功')).toBeVisible()
  })

  test('应该能够恢复默认配置', async ({ authenticatedPage }) => {
    const configPage = new SystemConfigPage(authenticatedPage)

    await configPage.goto()

    // 修改配置
    await configPage.updateConfig('maxUploadSize', '100')

    // 恢复默认
    await configPage.resetToDefault()

    // 验证恢复成功
    await expect(authenticatedPage.locator('text=恢复成功')).toBeVisible()
  })
})

test.describe('系统模块 - 审计日志', () => {
  test('应该能够查看审计日志', async ({ authenticatedPage }) => {
    const logPage = new AuditLogPage(authenticatedPage)

    await logPage.goto()
    await logPage.verifyPageLoaded()

    // 验证日志列表加载
    await expect(authenticatedPage.locator('.MuiDataGrid-root')).toBeVisible()
  })

  test('应该能够按用户筛选日志', async ({ authenticatedPage }) => {
    const logPage = new AuditLogPage(authenticatedPage)

    await logPage.goto()

    await logPage.filterByUser('admin')

    // 验证筛选结果
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够按操作类型筛选日志', async ({ authenticatedPage }) => {
    const logPage = new AuditLogPage(authenticatedPage)

    await logPage.goto()

    await logPage.filterByAction('创建')

    // 验证筛选结果
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够按日期范围筛选日志', async ({ authenticatedPage }) => {
    const logPage = new AuditLogPage(authenticatedPage)

    await logPage.goto()

    await logPage.filterByDateRange('2026-01-01', '2026-01-31')

    // 验证筛选结果
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够查看日志详情', async ({ authenticatedPage }) => {
    const logPage = new AuditLogPage(authenticatedPage)

    await logPage.goto()

    // 查看第一条日志详情（假设日志 ID 为 1）
    await logPage.viewLogDetail(1)

    // 验证详情显示
    await expect(authenticatedPage.locator('.log-detail')).toBeVisible()
  })

  test('应该能够导出日志', async ({ authenticatedPage }) => {
    const logPage = new AuditLogPage(authenticatedPage)

    await logPage.goto()

    await logPage.exportLogs()

    // 验证导出成功
    await expect(authenticatedPage.locator('text=导出成功')).toBeVisible()
  })
})

test.describe('系统模块 - 系统监控', () => {
  test('应该能够查看系统监控页面', async ({ authenticatedPage }) => {
    const monitorPage = new MonitoringPage(authenticatedPage)

    await monitorPage.goto()
    await monitorPage.verifyPageLoaded()

    // 验证监控页面加载
    await expect(authenticatedPage.locator('.monitoring-dashboard, .MuiGrid-root')).toBeVisible()
  })

  test('应该能够查看系统指标', async ({ authenticatedPage }) => {
    const monitorPage = new MonitoringPage(authenticatedPage)

    await monitorPage.goto()

    await monitorPage.viewSystemMetrics()

    // 验证指标图表显示
    await expect(authenticatedPage.locator('.metrics-chart')).toBeVisible()
  })

  test('应该能够查看 API 指标', async ({ authenticatedPage }) => {
    const monitorPage = new MonitoringPage(authenticatedPage)

    await monitorPage.goto()

    await monitorPage.viewApiMetrics()

    // 验证 API 指标显示
    await expect(authenticatedPage.locator('.api-metrics')).toBeVisible()
  })

  test('应该能够查看数据库指标', async ({ authenticatedPage }) => {
    const monitorPage = new MonitoringPage(authenticatedPage)

    await monitorPage.goto()

    await monitorPage.viewDatabaseMetrics()

    // 验证数据库指标显示
    await expect(authenticatedPage.locator('.db-metrics')).toBeVisible()
  })

  test('应该能够刷新监控数据', async ({ authenticatedPage }) => {
    const monitorPage = new MonitoringPage(authenticatedPage)

    await monitorPage.goto()

    await monitorPage.refreshMetrics()

    // 验证数据刷新
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够设置自动刷新', async ({ authenticatedPage }) => {
    const monitorPage = new MonitoringPage(authenticatedPage)

    await monitorPage.goto()

    await monitorPage.setAutoRefresh(30)

    // 验证设置成功
    await authenticatedPage.waitForLoadState('networkidle')
  })
})

test.describe('系统模块 - 告警管理', () => {
  test('应该能够创建新告警', async ({ authenticatedPage }) => {
    const alertPage = new AlertManagementPage(authenticatedPage)

    await alertPage.goto()
    await alertPage.verifyPageLoaded()

    await alertPage.createAlert({
      name: 'CPU 使用率告警',
      condition: 'CPU 使用率超过',
      threshold: 80,
    })

    // 验证告警创建成功
    await expect(authenticatedPage.locator('text=创建成功')).toBeVisible()
  })

  test('应该能够确认告警', async ({ authenticatedPage }) => {
    const alertPage = new AlertManagementPage(authenticatedPage)

    await alertPage.goto()

    // 确认第一条告警（假设告警 ID 为 1）
    await alertPage.acknowledgeAlert(1)

    // 验证确认成功
    await expect(authenticatedPage.locator('text=确认成功')).toBeVisible()
  })

  test('应该能够解决告警', async ({ authenticatedPage }) => {
    const alertPage = new AlertManagementPage(authenticatedPage)

    await alertPage.goto()

    // 解决第一条告警
    await alertPage.resolveAlert(1)

    // 验证解决成功
    await expect(authenticatedPage.locator('text=解决成功')).toBeVisible()
  })

  test('应该能够删除告警', async ({ authenticatedPage }) => {
    const alertPage = new AlertManagementPage(authenticatedPage)

    await alertPage.goto()

    // 创建临时告警
    await alertPage.createAlert({
      name: '临时告警',
      condition: '内存使用率超过',
      threshold: 90,
    })

    // 删除告警（假设告警 ID 为 2）
    await alertPage.deleteAlert(2)

    // 验证删除成功
    await expect(authenticatedPage.locator('text=删除成功')).toBeVisible()
  })
})
