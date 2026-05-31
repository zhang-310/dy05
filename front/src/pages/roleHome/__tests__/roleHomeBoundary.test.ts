import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'

function filesUnder(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) return filesUnder(full)
    return /\.(ts|tsx)$/.test(full) ? [full] : []
  })
}

describe('role home BFF boundary', () => {
  it('keeps role home pages on the roleHomeApi instead of stitching domain APIs directly', () => {
    const root = join(process.cwd(), 'src/pages/roleHome')
    const forbidden = [
      '@/api/dashboard',
      '@/api/ai',
      '@/api/live',
      '@/api/shortvideo',
      'dashboardApi',
      'aiApi',
      'liveApi',
      'shortvideoApi',
    ]

    const offenders = filesUnder(root)
      .filter((file) => !file.endsWith('roleHomeBoundary.test.ts'))
      .flatMap((file) => {
        const content = readFileSync(file, 'utf8')
        return forbidden
          .filter((needle) => content.includes(needle))
          .map((needle) => `${file}: ${needle}`)
      })

    expect(offenders).toEqual([])
  })
})
