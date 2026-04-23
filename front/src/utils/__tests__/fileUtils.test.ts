import { describe, it, expect } from 'vitest'
import { formatFileSize, formatSpeed, formatTime, splitFileIntoChunks } from '../fileUtils'

describe('formatFileSize', () => {
  it('formats 0 bytes', () => {
    expect(formatFileSize(0)).toBe('0 Bytes')
  })

  it('formats bytes', () => {
    expect(formatFileSize(500)).toBe('500 Bytes')
  })

  it('formats kilobytes', () => {
    expect(formatFileSize(1024)).toBe('1 KB')
  })

  it('formats megabytes', () => {
    expect(formatFileSize(1048576)).toBe('1 MB')
  })

  it('formats gigabytes', () => {
    expect(formatFileSize(1073741824)).toBe('1 GB')
  })

  it('formats with decimals', () => {
    expect(formatFileSize(1536)).toBe('1.5 KB')
  })
})

describe('formatSpeed', () => {
  it('formats bytes per second', () => {
    expect(formatSpeed(1024)).toBe('1 KB/s')
  })

  it('formats MB per second', () => {
    expect(formatSpeed(1048576)).toBe('1 MB/s')
  })
})

describe('formatTime', () => {
  it('formats seconds', () => {
    expect(formatTime(30)).toBe('30s')
  })

  it('formats minutes and seconds', () => {
    expect(formatTime(90)).toBe('1m 30s')
  })

  it('formats hours and minutes', () => {
    expect(formatTime(3660)).toBe('1h 1m')
  })

  it('rounds seconds', () => {
    expect(formatTime(0.7)).toBe('1s')
  })
})

describe('splitFileIntoChunks', () => {
  it('splits a small file into one chunk', () => {
    const file = new File(['hello'], 'test.txt', { type: 'text/plain' })
    const chunks = splitFileIntoChunks(file)
    expect(chunks.length).toBe(1)
  })

  it('returns empty array-like for empty file', () => {
    const file = new File([], 'empty.txt', { type: 'text/plain' })
    const chunks = splitFileIntoChunks(file)
    // ceil(0 / 5MB) = 0
    expect(chunks.length).toBe(0)
  })
})
