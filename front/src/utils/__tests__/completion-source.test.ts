import { describe, it, expect } from 'vitest'
import { completionSourceShortLabel, completionSourceChipColor } from '../completion-source'

describe('completionSourceShortLabel', () => {
  it('returns correct label for official', () => {
    expect(completionSourceShortLabel('douyin_official_completion')).toBe('官方适配')
  })

  it('returns correct label for retention_peak', () => {
    expect(completionSourceShortLabel('retention_peak')).toBe('监控推算')
  })

  it('returns correct label for monitor_derived', () => {
    expect(completionSourceShortLabel('monitor_derived')).toBe('面板采样')
  })

  it('returns correct label for configured_fallback', () => {
    expect(completionSourceShortLabel('configured_fallback')).toBe('配置回退')
  })

  it('returns correct label for legacy_unknown', () => {
    expect(completionSourceShortLabel('legacy_unknown')).toBe('历史未标')
  })

  it('returns correct label for none', () => {
    expect(completionSourceShortLabel('none')).toBe('无样本')
  })

  it('returns 代理 for undefined', () => {
    expect(completionSourceShortLabel(undefined)).toBe('代理')
  })

  it('returns 代理 for unknown values', () => {
    expect(completionSourceShortLabel('something_else')).toBe('代理')
  })
})

describe('completionSourceChipColor', () => {
  it('returns success for official', () => {
    expect(completionSourceChipColor('douyin_official_completion')).toBe('success')
  })

  it('returns primary for retention_peak', () => {
    expect(completionSourceChipColor('retention_peak')).toBe('primary')
  })

  it('returns success for monitor_derived', () => {
    expect(completionSourceChipColor('monitor_derived')).toBe('success')
  })

  it('returns warning for configured_fallback', () => {
    expect(completionSourceChipColor('configured_fallback')).toBe('warning')
  })

  it('returns warning for legacy_unknown', () => {
    expect(completionSourceChipColor('legacy_unknown')).toBe('warning')
  })

  it('returns error for none', () => {
    expect(completionSourceChipColor('none')).toBe('error')
  })

  it('returns default for undefined', () => {
    expect(completionSourceChipColor(undefined)).toBe('default')
  })
})
