import { describe, it, expect } from 'vitest'
import { echarts } from '../echarts-registry'

describe('echarts-registry', () => {
  it('exports echarts instance', () => {
    expect(echarts).toBeDefined()
  })

  it('has use method', () => {
    expect(typeof echarts.use).toBe('function')
  })

  it('has init method', () => {
    expect(typeof echarts.init).toBe('function')
  })
})
