import { test as base } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

/**
 * 自动修复失败测试的工具
 * 分析测试失败原因并生成修复建议
 */

interface FailureAnalysis {
  testName: string
  errorType: string
  errorMessage: string
  suggestions: string[]
  autoFixable: boolean
  fixCode?: string
}

export class TestAutoFixer {
  private failureLog: FailureAnalysis[] = []
  private logPath: string

  constructor(logPath: string = './test-results/autofix-log.json') {
    this.logPath = logPath
    this.loadFailureLog()
  }

  /**
   * 分析测试失败
   */
  analyzeFailure(testName: string, error: Error): FailureAnalysis {
    const errorMessage = error.message
    const errorType = this.categorizeError(errorMessage)
    const suggestions = this.generateSuggestions(errorType, errorMessage)
    const autoFixable = this.isAutoFixable(errorType)
    const fixCode = autoFixable ? this.generateFixCode(errorType, errorMessage) : undefined

    const analysis: FailureAnalysis = {
      testName,
      errorType,
      errorMessage,
      suggestions,
      autoFixable,
      fixCode,
    }

    this.failureLog.push(analysis)
    this.saveFailureLog()

    return analysis
  }

  /**
   * 分类错误类型
   */
  private categorizeError(errorMessage: string): string {
    if (errorMessage.includes('Timeout') || errorMessage.includes('timeout')) {
      return 'TIMEOUT'
    }
    if (errorMessage.includes('not visible') || errorMessage.includes('not found')) {
      return 'ELEMENT_NOT_FOUND'
    }
    if (errorMessage.includes('401') || errorMessage.includes('403') || errorMessage.includes('unauthorized')) {
      return 'AUTH_ERROR'
    }
    if (errorMessage.includes('detached') || errorMessage.includes('navigation')) {
      return 'NAVIGATION_ERROR'
    }
    if (errorMessage.includes('expected') && errorMessage.includes('received')) {
      return 'ASSERTION_ERROR'
    }
    if (errorMessage.includes('network') || errorMessage.includes('ERR_')) {
      return 'NETWORK_ERROR'
    }
    if (errorMessage.includes('obscured') || errorMessage.includes('intercepted')) {
      return 'ELEMENT_OBSCURED'
    }
    return 'UNKNOWN'
  }

  /**
   * 生成修复建议
   */
  private generateSuggestions(errorType: string, errorMessage: string): string[] {
    const suggestions: string[] = []

    switch (errorType) {
      case 'TIMEOUT':
        suggestions.push('增加超时时间: { timeout: 30000 }')
        suggestions.push('使用 waitForLoadState("networkidle")')
        suggestions.push('检查 API 响应是否过慢')
        suggestions.push('使用 expectWithRetry 进行重试')
        break

      case 'ELEMENT_NOT_FOUND':
        suggestions.push('检查选择器是否正确')
        suggestions.push('使用 waitForSelector 等待元素出现')
        suggestions.push('检查元素是否在 iframe 中')
        suggestions.push('使用更宽松的选择器（如 :has-text）')
        break

      case 'AUTH_ERROR':
        suggestions.push('使用 authenticatedPage fixture')
        suggestions.push('检查 token 是否过期')
        suggestions.push('在测试前重新登录')
        suggestions.push('使用 autoFixPage 自动处理登录')
        break

      case 'NAVIGATION_ERROR':
        suggestions.push('使用 waitForURL 等待导航完成')
        suggestions.push('检查是否有未保存的更改提示')
        suggestions.push('使用 page.reload() 刷新页面')
        break

      case 'ASSERTION_ERROR':
        suggestions.push('检查断言的预期值是否正确')
        suggestions.push('使用 toContainText 而非 toHaveText')
        suggestions.push('添加等待时间让数据加载完成')
        suggestions.push('使用 expectWithRetry 处理异步数据')
        break

      case 'NETWORK_ERROR':
        suggestions.push('检查后端服务是否运行')
        suggestions.push('检查 API 端点是否正确')
        suggestions.push('使用 mockApiResponse 模拟 API')
        suggestions.push('增加网络超时时间')
        break

      case 'ELEMENT_OBSCURED':
        suggestions.push('使用 scrollIntoViewIfNeeded()')
        suggestions.push('关闭遮罩层或对话框')
        suggestions.push('使用 force: true 强制点击')
        suggestions.push('使用 autoFixPage 自动关闭遮罩层')
        break

      default:
        suggestions.push('查看完整错误堆栈')
        suggestions.push('使用 --debug 模式调试')
        suggestions.push('检查测试环境配置')
    }

    return suggestions
  }

  /**
   * 判断是否可以自动修复
   */
  private isAutoFixable(errorType: string): boolean {
    return ['TIMEOUT', 'ELEMENT_NOT_FOUND', 'AUTH_ERROR', 'ELEMENT_OBSCURED'].includes(errorType)
  }

  /**
   * 生成修复代码
   */
  private generateFixCode(errorType: string, errorMessage: string): string {
    switch (errorType) {
      case 'TIMEOUT':
        return `
// 修复建议：增加超时时间并使用重试
await expectWithRetry(async () => {
  await expect(page.locator('selector')).toBeVisible()
}, { maxRetries: 5, retryDelay: 2000 })
`

      case 'ELEMENT_NOT_FOUND':
        return `
// 修复建议：等待元素出现
await page.waitForSelector('selector', { state: 'visible', timeout: 10000 })
await page.locator('selector').click()
`

      case 'AUTH_ERROR':
        return `
// 修复建议：使用 autoFixPage 自动处理登录
import { test } from '../fixtures/autofix.fixture'

test('测试名称', async ({ autoFixPage }) => {
  await autoFixPage.goto('/admin/dashboard')
  // autoFixPage 会自动检测并处理登录
})
`

      case 'ELEMENT_OBSCURED':
        return `
// 修复建议：滚动到元素并关闭遮罩层
await page.locator('selector').scrollIntoViewIfNeeded()
await page.keyboard.press('Escape') // 关闭可能的遮罩层
await page.locator('selector').click()
`

      default:
        return '// 无自动修复代码'
    }
  }

  /**
   * 保存失败日志
   */
  private saveFailureLog() {
    const dir = path.dirname(this.logPath)
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true })
    }
    fs.writeFileSync(this.logPath, JSON.stringify(this.failureLog, null, 2))
  }

  /**
   * 加载失败日志
   */
  private loadFailureLog() {
    if (fs.existsSync(this.logPath)) {
      const content = fs.readFileSync(this.logPath, 'utf-8')
      this.failureLog = JSON.parse(content)
    }
  }

  /**
   * 生成修复报告
   */
  generateReport(): string {
    let report = '# 测试失败自动修复报告\n\n'
    report += `生成时间: ${new Date().toLocaleString()}\n\n`
    report += `总失败数: ${this.failureLog.length}\n`
    report += `可自动修复: ${this.failureLog.filter(f => f.autoFixable).length}\n\n`

    // 按错误类型分组
    const groupedByType = this.failureLog.reduce((acc, failure) => {
      if (!acc[failure.errorType]) {
        acc[failure.errorType] = []
      }
      acc[failure.errorType].push(failure)
      return acc
    }, {} as Record<string, FailureAnalysis[]>)

    for (const [errorType, failures] of Object.entries(groupedByType)) {
      report += `## ${errorType} (${failures.length} 个)\n\n`

      for (const failure of failures) {
        report += `### ${failure.testName}\n\n`
        report += `**错误信息:**\n\`\`\`\n${failure.errorMessage}\n\`\`\`\n\n`
        report += `**修复建议:**\n`
        failure.suggestions.forEach(s => {
          report += `- ${s}\n`
        })
        report += '\n'

        if (failure.fixCode) {
          report += `**修复代码:**\n\`\`\`typescript\n${failure.fixCode}\n\`\`\`\n\n`
        }

        report += '---\n\n'
      }
    }

    return report
  }

  /**
   * 清除失败日志
   */
  clearLog() {
    this.failureLog = []
    if (fs.existsSync(this.logPath)) {
      fs.unlinkSync(this.logPath)
    }
  }
}

/**
 * Playwright 测试钩子，自动分析失败
 */
export const autoFixTest = base.extend({
  page: async ({ page }, use, testInfo) => {
    const fixer = new TestAutoFixer()

    try {
      await use(page)
    } catch (error) {
      // 分析失败
      const analysis = fixer.analyzeFailure(testInfo.title, error as Error)

      // 输出修复建议
      console.log('\n========== 自动修复建议 ==========')
      console.log(`测试: ${analysis.testName}`)
      console.log(`错误类型: ${analysis.errorType}`)
      console.log(`可自动修复: ${analysis.autoFixable ? '是' : '否'}`)
      console.log('\n修复建议:')
      analysis.suggestions.forEach((s, i) => {
        console.log(`  ${i + 1}. ${s}`)
      })

      if (analysis.fixCode) {
        console.log('\n修复代码:')
        console.log(analysis.fixCode)
      }
      console.log('===================================\n')

      throw error
    }
  },
})
