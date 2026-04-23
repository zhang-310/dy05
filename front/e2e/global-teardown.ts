/**
 * Playwright 全局清理
 * 在所有测试运行后执行
 *
 * 功能：
 * 1. 停止自动启动的后端服务
 * 2. 生成测试报告
 * 3. 清理临时文件
 */

import * as fs from 'fs'
import * as path from 'path'
import { exec } from 'child_process'
import { promisify } from 'util'
import { TestAutoFixer } from './utils/test-fixer'

const execAsync = promisify(exec)

async function globalTeardown() {
  console.log('========================================')
  console.log('🧹 开始清理测试环境')
  console.log('========================================')

  // 停止自动启动的后端服务
  const autoStartBackend = process.env.E2E_AUTO_START_BACKEND !== 'false'
  if (autoStartBackend) {
    await stopBackendService()
  }

  // 生成自动修复报告
  const fixer = new TestAutoFixer()
  const report = fixer.generateReport()

  const reportPath = path.join(__dirname, '../test-results/autofix-report.md')
  fs.writeFileSync(reportPath, report)
  console.log(`📊 自动修复报告已生成: ${reportPath}`)

  // 统计测试结果
  const resultsPath = path.join(__dirname, '../test-results/results.json')
  if (fs.existsSync(resultsPath)) {
    const results = JSON.parse(fs.readFileSync(resultsPath, 'utf-8'))
    const stats = {
      total: results.suites?.reduce((sum: number, suite: any) => sum + (suite.specs?.length || 0), 0) || 0,
      passed: 0,
      failed: 0,
      skipped: 0,
      duration: 0,
    }

    // 递归统计
    function countTests(suite: any) {
      if (suite.specs) {
        suite.specs.forEach((spec: any) => {
          spec.tests?.forEach((test: any) => {
            if (test.status === 'passed') stats.passed++
            else if (test.status === 'failed') stats.failed++
            else if (test.status === 'skipped') stats.skipped++
            stats.duration += test.results?.[0]?.duration || 0
          })
        })
      }
      if (suite.suites) {
        suite.suites.forEach(countTests)
      }
    }

    results.suites?.forEach(countTests)

    console.log('\n========================================')
    console.log('📈 测试统计')
    console.log('========================================')
    console.log(`总测试数: ${stats.total}`)
    console.log(`✅ 通过: ${stats.passed}`)
    console.log(`❌ 失败: ${stats.failed}`)
    console.log(`⏭️  跳过: ${stats.skipped}`)
    console.log(`⏱️  总耗时: ${(stats.duration / 1000).toFixed(2)}s`)
    console.log(`📊 通过率: ${((stats.passed / stats.total) * 100).toFixed(2)}%`)
    console.log('========================================')

    // 生成简要报告
    const summaryPath = path.join(__dirname, '../test-results/summary.txt')
    const summary = `
E2E 测试总结
============

测试时间: ${new Date().toLocaleString()}

测试统计:
- 总测试数: ${stats.total}
- 通过: ${stats.passed}
- 失败: ${stats.failed}
- 跳过: ${stats.skipped}
- 总耗时: ${(stats.duration / 1000).toFixed(2)}s
- 通过率: ${((stats.passed / stats.total) * 100).toFixed(2)}%

详细报告:
- HTML 报告: playwright-report/index.html
- JSON 结果: test-results/results.json
- JUnit XML: test-results/junit.xml
- 自动修复报告: test-results/autofix-report.md

查看 HTML 报告:
  npx playwright show-report

查看自动修复建议:
  cat test-results/autofix-report.md
`
    fs.writeFileSync(summaryPath, summary)
    console.log(`\n📄 测试总结已保存: ${summaryPath}`)
  }

  // 清理临时文件（可选）
  // const tempDir = path.join(__dirname, '../.temp')
  // if (fs.existsSync(tempDir)) {
  //   fs.rmSync(tempDir, { recursive: true, force: true })
  //   console.log('🗑️  临时文件已清理')
  // }

  console.log('\n✅ 全局清理完成')
  console.log('========================================\n')
}

/**
 * 停止后端服务
 */
async function stopBackendService(): Promise<void> {
  const pidFile = path.join(__dirname, '.backend-pid')

  if (!fs.existsSync(pidFile)) {
    console.log('ℹ️  未找到后端进程 PID 文件，跳过停止')
    return
  }

  try {
    const pid = fs.readFileSync(pidFile, 'utf-8').trim()
    console.log(`🛑 停止后端服务 (PID: ${pid})`)

    if (process.platform === 'win32') {
      // Windows: 使用 taskkill
      await execAsync(`taskkill /F /PID ${pid} /T`)
    } else {
      // Unix: 使用 kill
      await execAsync(`kill -9 ${pid}`)
    }

    console.log('✅ 后端服务已停止')
  } catch (error) {
    // 进程可能已经停止，忽略错误
    console.log('ℹ️  后端进程可能已停止')
  } finally {
    // 删除 PID 文件
    try {
      fs.unlinkSync(pidFile)
    } catch (error) {
      // 忽略删除错误
    }
  }
}

export default globalTeardown
