import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/utils'
import MarkdownViewer from '../MarkdownViewer'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

describe('MarkdownViewer', () => {
  it('renders headings, gfm tables, task lists, code and links', () => {
    renderWithProviders(
      <MarkdownViewer
        content={[
          '# 文档标题',
          '',
          '- [x] 已完成',
          '- [ ] 待处理',
          '',
          '| 字段 | 说明 |',
          '| --- | --- |',
          '| title | 标题 |',
          '',
          '`inline`',
          '',
          '```ts',
          'const ok = true',
          '```',
          '',
          '[OpenAI](https://openai.com)',
          '[相对链接](/admin/ai/knowledge)',
          '[危险链接](javascript:alert(1))',
          '',
          '![知识图谱](/tmp/audit-screenshots/markdown-safe-image.svg)',
        ].join('\n')}
      />,
    )

    const viewer = screen.getByTestId('markdown-viewer')
    expect(screen.getByRole('heading', { name: '文档标题' })).toBeInTheDocument()
    expect(screen.getByRole('table')).toBeInTheDocument()
    expect(screen.getByText('const ok = true')).toBeInTheDocument()
    expect(viewer).toHaveAttribute('data-renderer-contract', 'react-markdown+remark-gfm')
    expect(viewer).toHaveAttribute('data-safe-url-transform', 'react-markdown-defaultUrlTransform')
    expect(viewer).toHaveAttribute('data-no-static-markdown-render', 'true')
    expect(screen.getByRole('link', { name: 'OpenAI' })).toHaveAttribute('target', '_blank')
    expect(screen.getByRole('link', { name: 'OpenAI' })).toHaveAttribute('rel', 'noopener noreferrer')
    expect(screen.getByRole('link', { name: '相对链接' })).not.toHaveAttribute('target')
    expect(screen.getByText('危险链接').closest('a')).not.toHaveAttribute('href')
    expect(screen.getByRole('checkbox', { checked: true })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: '知识图谱' })).toHaveAttribute('loading', 'lazy')
    expect(viewer.querySelector('[data-markdown-node="table-scroll"]')).toBeInTheDocument()
    expect(viewer.querySelector('[data-markdown-node="code-block"]')).toBeInTheDocument()
  })

  it('uses readable dark theme surfaces for code blocks, quotes and tables', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MarkdownViewer
          content={[
            '> 引用说明',
            '',
            '| 字段 | 说明 |',
            '| --- | --- |',
            '| score | 质量分 |',
            '',
            '```json',
            '{"ok":true}',
            '```',
          ].join('\n')}
        />
      </AppThemeProvider>,
    )

    const viewer = screen.getByTestId('markdown-viewer')
    const pre = viewer.querySelector('pre') as HTMLElement
    const blockquote = viewer.querySelector('blockquote') as HTMLElement
    const th = viewer.querySelector('th') as HTMLElement
    expect(window.getComputedStyle(pre).backgroundColor).toBe('rgb(18, 18, 18)')
    expect(window.getComputedStyle(pre).color).toBe('rgb(255, 255, 255)')
    expect(window.getComputedStyle(blockquote).backgroundColor).not.toBe('rgba(0, 0, 0, 0)')
    expect(window.getComputedStyle(th).backgroundColor).toBe('rgb(18, 18, 18)')
  })
})
