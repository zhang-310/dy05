import { describe, it, expect } from 'vitest'
import {
  listPerformanceCues,
  parsePerformanceCueSegments,
} from '../performanceCueText'

describe('performanceCueText', () => {
  it('parses cues with inner 2–30 chars', () => {
    const s = '开场【微笑看向镜头】接着说【停顿两秒】结束'
    const segs = parsePerformanceCueSegments(s)
    expect(segs).toEqual([
      { kind: 'text', text: '开场' },
      { kind: 'cue', text: '微笑看向镜头' },
      { kind: 'text', text: '接着说' },
      { kind: 'cue', text: '停顿两秒' },
      { kind: 'text', text: '结束' },
    ])
    expect(listPerformanceCues(s)).toEqual(['微笑看向镜头', '停顿两秒'])
  })

  it('ignores single-char inner brackets', () => {
    const s = '试【短】不算'
    expect(parsePerformanceCueSegments(s)).toEqual([{ kind: 'text', text: s }])
    expect(listPerformanceCues(s)).toEqual([])
  })

  it('handles empty input', () => {
    expect(parsePerformanceCueSegments('')).toEqual([])
    expect(listPerformanceCues('')).toEqual([])
  })
})
