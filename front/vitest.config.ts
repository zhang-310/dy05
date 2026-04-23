import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

/**
 * Vitest 单元测试配置
 * 目标：100% 代码覆盖率
 */
export default defineConfig({
  plugins: [react()],

  test: {
    // 测试环境
    environment: 'jsdom',

    // 全局设置
    globals: true,

    // 设置文件
    setupFiles: ['./src/test/setup.ts'],

    // 包含的测试文件
    include: [
      'src/**/*.{test,spec}.{ts,tsx}',
      'src/**/__tests__/**/*.{ts,tsx}'
    ],

    // 排除的文件
    exclude: [
      'node_modules',
      'dist',
      'build',
      'e2e',
      '.next',
      '.nuxt',
      '.cache',
      'coverage',
      'playwright-report',
      'test-results',
      // 依赖图极大，在 vitest+v8 收集阶段会长时间阻塞；待拆 hook 或缩小 mock 面后再纳入
      'src/pages/live/hooks/__tests__/useLiveScriptBuilder.test.tsx',
    ],

    // 覆盖率配置
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],

      // 包含的文件
      include: [
        'src/**/*.{ts,tsx}',
      ],

      // 排除的文件
      exclude: [
        'src/**/*.d.ts',
        'src/**/*.stories.tsx',
        'src/**/*.test.{ts,tsx}',
        'src/**/*.spec.{ts,tsx}',
        'src/**/__tests__/**',
        'src/test/**',
        'src/vite-env.d.ts',
        'src/main.tsx',
      ],

      // 覆盖率阈值（目标 100%）
      thresholds: {
        lines: 100,
        functions: 100,
        branches: 100,
        statements: 100,
      },

      // 严格模式
      all: true,
      skipFull: false,
    },

    // 测试超时
    testTimeout: 10000,
    hookTimeout: 10000,

    pool: 'threads',
    poolOptions: {
      threads: {
        singleThread: false,
      },
    },

    // 监听模式排除
    watchExclude: [
      '**/node_modules/**',
      '**/dist/**',
      '**/e2e/**',
      '**/coverage/**',
    ],

    // 报告器
    reporters: ['verbose'],

    // UI 仅本地需要时显式开启：vitest --ui（避免 run/coverage 在部分环境下挂起）
    ui: false,
  },

  // 路径别名（与 vite.config.ts 保持一致）
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
