import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { within } from '@testing-library/react'
import DouyinCookieManagePage from '../DouyinCookieManagePage'
import { douyinCookieApi } from '@/api/benchmark'

vi.mock('@/api/benchmark', () => ({
  douyinCookieApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    validate: vi.fn(),
    qrLoginStart: vi.fn(),
    qrLoginPoll: vi.fn(),
    qrLoginCancel: vi.fn(),
  },
}))

vi.mock('notistack', () => ({
  useSnackbar: () => ({ enqueueSnackbar: vi.fn() }),
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, loading }: any) => (
      <div>
        {loading && <span>loading</span>}
        <table>
          <tbody>
            {rows.map((row: any) => (
              <tr key={row.id}>
                {columns.map((col: any) => (
                  <td key={col.field}>
                    {col.renderCell
                      ? col.renderCell({ row, value: row[col.field] })
                      : String(row[col.field] ?? '')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ),
  }
})

describe('DouyinCookieManagePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(douyinCookieApi.list).mockResolvedValue({
      total: 2,
      list: [
        {
          id: 1,
          cookieName: '主采集 Cookie',
          cookieValue: 'a=b;c=d',
          isValid: true,
          useCount: 8,
        },
        {
          id: 2,
          cookieName: '失效 Cookie',
          cookieValue: 'x=y',
          isValid: false,
          useCount: 0,
        },
      ],
      pageNum: 0,
      pageSize: 30,
    } as never)
  })

  it('loads cookies and shows collection dependency diagnostics', async () => {
    renderWithProviders(
      <MemoryRouter>
        <DouyinCookieManagePage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '抖音 Cookie 管理' })).toBeInTheDocument()
    const page = screen.getByTestId('douyin-cookie-page')
    expect(page).toHaveAttribute('data-contract-scope', 'benchmark-douyin-cookie-server-browser-session')
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/cookie/list'))
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/cookie/qr-login/start'))
    expect(page).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/benchmark/cookie/local-list'))
    expect(page).toHaveAttribute('data-no-local-cookie-fallback', 'true')
    expect(page).toHaveAttribute('data-server-pagination', 'true')

    await waitFor(() => {
      expect(douyinCookieApi.list).toHaveBeenCalled()
    })

    expect(await screen.findByText('主采集 Cookie')).toBeInTheDocument()
    expect(screen.getByText('采集链路依赖说明')).toBeInTheDocument()
    expect(screen.getAllByText('失效').length).toBeGreaterThan(0)
  })

  it('renders normalized SDK page results without local wrapper parsing', async () => {
    vi.mocked(douyinCookieApi.list).mockResolvedValue({
      total: 7,
      list: [
        {
          id: 8,
          cookieName: '包装 Cookie',
          cookieValue: 'p=q',
          isValid: true,
          useCount: 3,
        },
      ],
      pageNum: 0,
      pageSize: 30,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <DouyinCookieManagePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装 Cookie')).toBeInTheDocument()
    expect(screen.getByText('7')).toBeInTheDocument()
  })

  it('keeps save dialog open when backend save fails', async () => {
    vi.mocked(douyinCookieApi.save).mockRejectedValue(new Error('write rejected') as never)

    renderWithProviders(
      <MemoryRouter>
        <DouyinCookieManagePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('主采集 Cookie')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '手动添加' }))
    const saveDialog = await screen.findByRole('dialog', { name: '添加Cookie' })
    fireEvent.change(within(saveDialog).getByLabelText(/Cookie名称/), { target: { value: '新 Cookie' } })
    fireEvent.change(within(saveDialog).getByLabelText(/Cookie 请求头/), { target: { value: 'sid=1' } })
    fireEvent.click(within(saveDialog).getByRole('button', { name: '保存' }))
    expect(await screen.findByText('保存失败：write rejected')).toBeInTheDocument()
    expect(screen.getByTestId('douyin-cookie-action-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('douyin-cookie-action-error')).toHaveAttribute('data-no-local-cookie-mutation', 'true')
    expect(screen.getByRole('dialog', { name: '添加Cookie' })).toBeInTheDocument()
  })

  it('keeps delete dialog open when backend delete fails', async () => {
    vi.mocked(douyinCookieApi.delete).mockRejectedValue(new Error('delete rejected') as never)

    renderWithProviders(
      <MemoryRouter>
        <DouyinCookieManagePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('主采集 Cookie')).toBeInTheDocument()
    fireEvent.click(screen.getAllByRole('button', { name: '删除' })[0])
    const deleteDialog = await screen.findByRole('dialog', { name: '确认删除' })
    fireEvent.click(within(deleteDialog).getByRole('button', { name: '确认' }))
    expect(await screen.findByText('删除失败：delete rejected')).toBeInTheDocument()
    expect(screen.getByTestId('douyin-cookie-action-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByRole('dialog', { name: '确认删除' })).toBeInTheDocument()
  })

  it('shows validate and QR start failures inline', async () => {
    vi.mocked(douyinCookieApi.validate).mockRejectedValue(new Error('cookie validate down') as never)
    vi.mocked(douyinCookieApi.qrLoginStart).mockRejectedValue(new Error('playwright unavailable') as never)

    renderWithProviders(
      <MemoryRouter>
        <DouyinCookieManagePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('主采集 Cookie')).toBeInTheDocument()
    fireEvent.click(screen.getAllByRole('button', { name: '验证' })[0])

    expect(await screen.findByText('验证失败：cookie validate down')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '扫码登录获取 Cookie' }))
    expect(await screen.findByText('扫码登录启动失败：playwright unavailable')).toBeInTheDocument()
  })
})
