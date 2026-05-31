import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ComplianceCheckPage from '../ComplianceCheckPage'
import {
  scriptComplianceCheck,
  scriptComplianceIndustryCodes,
  scriptComplianceRules,
  douyinOfficialReferences,
} from '@/api/compliance'

vi.mock('@/api/compliance', () => ({
  scriptComplianceCheck: vi.fn(),
  scriptComplianceIndustryCodes: vi.fn(),
  scriptComplianceRules: vi.fn(),
  douyinOfficialReferences: vi.fn(),
}))

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <ComplianceCheckPage />
    </MemoryRouter>,
  )
}

describe('ComplianceCheckPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(scriptComplianceIndustryCodes).mockResolvedValue({
      verticalCodes: ['cosmetics', 'food'],
      defaultVerticalCode: 'cosmetics',
      note: '叠加通用规则',
    })
    vi.mocked(scriptComplianceRules).mockResolvedValue([
      { pattern: '最好', level: 'error', reason: '绝对化', reference: '广告法', source: 'industry' },
    ])
    vi.mocked(douyinOfficialReferences).mockResolvedValue({
      notice: '公开规则',
      referenceUrls: ['https://example.com'],
    })
    vi.mocked(scriptComplianceCheck).mockResolvedValue([
      {
        matchedText: '最好',
        level: 'error',
        reason: '绝对化用语',
        reference: '广告法',
        position: 2,
        source: 'industry',
      },
    ])
  })

  it('uses script compliance SDK and renders violations', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: '合规检测' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('待检测文本'), { target: { value: '这是最好产品' } })
    fireEvent.click(screen.getByRole('button', { name: '检测合规' }))

    await waitFor(() => {
      expect(scriptComplianceCheck).toHaveBeenCalledWith({ text: '这是最好产品', industryCode: 'cosmetics' })
    })
    expect(await screen.findByText('最好')).toBeInTheDocument()
    expect(screen.getByText('绝对化用语')).toBeInTheDocument()
    expect(screen.getByTestId('compliance-check-page-workbench')).toHaveAttribute('data-contract-scope', 'script-compliance-industry-check')
    expect(screen.getByTestId('compliance-check-page-workbench')).toHaveAttribute('data-no-general-compliance-endpoint', 'true')
    expect(screen.getByTestId('compliance-check-page-workbench').getAttribute('data-unsupported-endpoints')).toContain('/compliance/check')
    expect(screen.getByTestId('compliance-source-contract')).toHaveAttribute('data-no-general-compliance-endpoint', 'true')
    expect(screen.getByTestId('compliance-source-contract')).toHaveAttribute('data-no-local-official-reference-fallback', 'true')
    expect(screen.getByTestId('compliance-result-contract')).toHaveAttribute('data-result-count', '1')
    expect(screen.getByTestId('compliance-result-contract')).toHaveAttribute('data-no-client-side-rule-match', 'true')
  })

  it('shows check error and retry action', async () => {
    vi.mocked(scriptComplianceCheck).mockRejectedValue(new Error('compliance down'))

    renderPage()

    fireEvent.change(screen.getByLabelText('待检测文本'), { target: { value: '测试' } })
    fireEvent.click(screen.getByRole('button', { name: '检测合规' }))

    const alert = await screen.findByText('合规检测失败')
    expect(alert).toBeInTheDocument()
    expect(screen.getByText('compliance down')).toBeInTheDocument()
    expect(screen.getByTestId('compliance-check-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('compliance-check-error')).toHaveAttribute('data-no-local-violation-fallback', 'true')

    vi.mocked(scriptComplianceCheck).mockResolvedValue([])
    fireEvent.click(within(alert.closest('.MuiAlert-root') as HTMLElement).getByRole('button', { name: '重试' }))

    await waitFor(() => {
      expect(scriptComplianceCheck).toHaveBeenCalledTimes(2)
    })
  })

  it('uses static industry selector fallback only when industry code API fails', async () => {
    vi.mocked(scriptComplianceIndustryCodes).mockRejectedValue(new Error('industry down') as never)
    vi.mocked(scriptComplianceCheck).mockResolvedValue([])

    renderPage()

    expect(await screen.findByTestId('compliance-industry-codes-error')).toHaveAttribute('data-fallback-source', 'static-selector-only')
    expect(screen.getByTestId('compliance-source-contract')).toHaveAttribute('data-industry-code-source', 'static-selector-fallback')

    fireEvent.change(screen.getByLabelText('待检测文本'), { target: { value: '正常文案' } })
    fireEvent.click(screen.getByRole('button', { name: '检测合规' }))

    expect(await screen.findByTestId('compliance-check-clean')).toHaveAttribute('data-no-local-violation-fallback', 'true')
    await waitFor(() => {
      expect(scriptComplianceCheck).toHaveBeenCalledWith({ text: '正常文案', industryCode: 'cosmetics' })
    })
  })

  it('shows official reference error without local official-rule fallback', async () => {
    vi.mocked(douyinOfficialReferences).mockRejectedValue(new Error('reference down') as never)

    renderPage()

    expect(await screen.findByTestId('compliance-official-references-error')).toHaveAttribute('data-no-local-official-reference-fallback', 'true')
    expect(screen.getByText(/不会用本地摘要伪造官方规则/)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('待检测文本'), { target: { value: '这是最好产品' } })
    fireEvent.click(screen.getByRole('button', { name: '检测合规' }))

    await waitFor(() => {
      expect(scriptComplianceCheck).toHaveBeenCalledWith({ text: '这是最好产品', industryCode: 'cosmetics' })
    })
  })
})
