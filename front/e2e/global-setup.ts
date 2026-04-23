/**
 * Playwright 全局设置
 * 在所有测试运行前执行
 *
 * 功能：
 * 1. 自动启动后端服务（如果未运行）
 * 2. 等待后端健康检查通过
 * 3. 初始化测试数据库
 * 4. 设置测试环境变量
 */

import { chromium, FullConfig } from '@playwright/test'
import { spawn, ChildProcess } from 'child_process'
import * as path from 'path'
import * as fs from 'fs'

// 全局变量存储后端进程
let backendProcess: ChildProcess | null = null

async function globalSetup(config: FullConfig) {
  console.log('========================================')
  console.log('🚀 开始 E2E 测试全局设置')
  console.log('========================================')

  // 设置环境变量
  process.env.TEST_USERNAME = process.env.TEST_USERNAME || 'admin'
  process.env.TEST_PASSWORD = process.env.TEST_PASSWORD || 'admin123'
  process.env.SPRING_PROFILES_ACTIVE = 'test'

  const backendUrl = process.env.BACKEND_URL || 'http://localhost:8080'
  const autoStartBackend = process.env.E2E_AUTO_START_BACKEND !== 'false' // 默认启动

  console.log(`🔍 检查后端服务: ${backendUrl}`)
  console.log(`⚙️  自动启动后端: ${autoStartBackend ? '是' : '否'}`)

  // 检查后端是否已经运行
  const isBackendRunning = await checkBackendHealth(backendUrl)

  if (!isBackendRunning && autoStartBackend) {
    console.log('🚀 后端服务未运行，正在启动...')
    await startBackendService()
  } else if (!isBackendRunning && !autoStartBackend) {
    console.error('❌ 后端服务未启动，请先启动后端服务:')
    console.error('   cd C:\\claude\\dy02')
    console.error('   mvn spring-boot:run')
    throw new Error('Backend service is not running. Please start the backend before running E2E tests.')
  } else {
    console.log('✅ 后端服务已在运行')
  }

  // 等待后端健康检查通过
  await waitForBackendReady(backendUrl)

  console.log('✅ 全局设置完成')
  console.log('')
}

/**
 * 检查后端健康状态
 */
async function checkBackendHealth(backendUrl: string): Promise<boolean> {
  try {
    const browser = await chromium.launch()
    const page = await browser.newPage()

    const response = await page.goto(`${backendUrl}/actuator/health`, {
      timeout: 3000,
      waitUntil: 'domcontentloaded'
    })

    await browser.close()

    return response !== null && response.ok()
  } catch (error) {
    return false
  }
}

/**
 * 启动后端服务
 */
async function startBackendService(): Promise<void> {
  const projectRoot = path.resolve(__dirname, '../..')
  const mvnCommand = process.platform === 'win32' ? 'mvn.cmd' : 'mvn'

  console.log(`📂 项目根目录: ${projectRoot}`)
  console.log(`🔧 Maven 命令: ${mvnCommand}`)

  // 检查 pom.xml 是否存在
  const pomPath = path.join(projectRoot, 'pom.xml')
  if (!fs.existsSync(pomPath)) {
    throw new Error(`pom.xml not found at ${pomPath}`)
  }

  // 启动 Spring Boot 应用（测试模式）
  backendProcess = spawn(
    mvnCommand,
    ['spring-boot:run', '-Dspring-boot.run.profiles=test'],
    {
      cwd: projectRoot,
      stdio: ['ignore', 'pipe', 'pipe'],
      shell: true,
      detached: false
    }
  )

  // 保存进程 ID 到文件，用于清理
  const pidFile = path.join(__dirname, '.backend-pid')
  fs.writeFileSync(pidFile, String(backendProcess.pid))

  // 监听输出
  backendProcess.stdout?.on('data', (data) => {
    const output = data.toString()
    if (output.includes('Started') || output.includes('Tomcat started')) {
      console.log('✅ 后端服务启动成功')
    }
  })

  backendProcess.stderr?.on('data', (data) => {
    const error = data.toString()
    // 只记录严重错误，忽略警告
    if (error.includes('ERROR') || error.includes('FATAL')) {
      console.error('❌ 后端启动错误:', error)
    }
  })

  backendProcess.on('error', (error) => {
    console.error('❌ 后端进程错误:', error)
  })

  backendProcess.on('exit', (code) => {
    if (code !== 0 && code !== null) {
      console.error(`❌ 后端进程退出，代码: ${code}`)
    }
  })

  console.log(`🔄 后端服务启动中... (PID: ${backendProcess.pid})`)
}

/**
 * 等待后端服务就绪
 */
async function waitForBackendReady(backendUrl: string): Promise<void> {
  const maxRetries = 60 // 增加到 60 次（2 分钟）
  const retryDelay = 2000 // 2 秒

  for (let i = 0; i < maxRetries; i++) {
    try {
      const browser = await chromium.launch()
      const page = await browser.newPage()

      const response = await page.goto(`${backendUrl}/actuator/health`, {
        timeout: 5000,
        waitUntil: 'domcontentloaded'
      })

      await browser.close()

      if (response && response.ok()) {
        console.log('✅ 后端服务健康检查通过')
        return
      }
    } catch (error) {
      if (i < maxRetries - 1) {
        process.stdout.write(`⏳ 等待后端服务就绪... (${i + 1}/${maxRetries})\r`)
        await new Promise(resolve => setTimeout(resolve, retryDelay))
      } else {
        console.error('\n❌ 后端服务健康检查超时')
        throw new Error('Backend service health check timeout after 2 minutes')
      }
    }
  }
}

export default globalSetup
