import { beforeEach, describe, expect, it, vi } from 'vitest'
import { waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen } from '@/test/utils'
import RoleHomePage from '../RoleHomePage'
import { roleHomeApi, type RoleHomeKind } from '@/api/role-home'

const navigate = vi.hoisted(() => vi.fn())

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('@/api/role-home', () => ({
  roleHomeApi: {
    getHome: vi.fn(),
  },
}))

function renderRole(role: 'admin' | 'org' | 'talent' | 'user') {
  return renderWithProviders(
    <MemoryRouter>
      <RoleHomePage role={role} />
    </MemoryRouter>,
  )
}

describe('RoleHomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    navigate.mockClear()
    vi.mocked(roleHomeApi.getHome).mockImplementation(async (role: RoleHomeKind) => {
      const base = {
        role,
        userId: 7,
        roleCode: role === 'org' ? 'institution' : role,
        organizationId: role === 'org' ? 88 : null,
        visibleOwnerIds: role === 'admin' ? null : [7],
        boundary: {
          unrestricted: role === 'admin',
          visibleOwnerIds: role === 'admin' ? null : [7],
          requiresOwnerFilter: role !== 'admin',
        },
        readyEndpoints: [`/api/v1/${role}/home`],
        sections: [],
        businessChains: [
          {
            key: `${role}-official-chain`,
            title: role === 'org' ? '直播话术生成/微调' : '短视频脚本/分镜',
            path: `/${role}/official-chain`,
            requiredKnowledge: 'douyin,douyin_weigui,viral_patterns',
            guardRequired: role !== 'admin',
          },
        ],
        knowledgeGuard: {
          requiredKbCodes: ['douyin', 'douyin_weigui'],
          officialReferenceRequired: true,
          blockWithoutOfficialReference: role !== 'admin',
          appliesTo: ['shortvideo-script', 'live-script', 'publish-review'],
          status: 'contract-ready',
          message: '业务链路必须检索 douyin + douyin_weigui，并在 AI 输出中暴露官方规则引用。',
        },
        learningLoops: [
          {
            key: `${role}-learning-loop`,
            title: '爆款采集结果结构化入库',
            targetKb: 'viral_patterns',
            source: 'benchmark_collect_result',
            status: 'contract-ready',
          },
        ],
      }
      if (role === 'admin') {
        return {
          ...base,
          metrics: {
            totalUsers: 100,
            todayUsers: 3,
            todayAiCalls: 88,
            aiSuccessRate: 96,
            todayRevenue: 12000,
            kbDocumentCount: 66,
          },
        }
      }
      if (role === 'org') {
        return {
          ...base,
          metrics: {
            totalLiveSessions: 8,
            todaySessions: 1,
            todayRevenue: 9000,
            pendingApprovalCount: 1,
          },
        }
      }
      return {
        ...base,
        metrics: {
          projectCount: 1,
          latestProjectTitle: '个人测评短视频',
          totalVideoCount: 3,
          totalPlayCount: 12000,
          totalLiveSessions: role === 'talent' ? 2 : 0,
        },
      }
    })
  })

  it('renders admin home with admin-only endpoints', async () => {
    renderRole('admin')

    const page = await screen.findByTestId('admin-home-page')
    expect(await screen.findByText('平台总控台')).toBeInTheDocument()
    await waitFor(() => expect(page).toHaveAttribute('data-owner-boundary', '全局可见'))
    expect(page).toHaveAttribute('data-shell-path', '/admin')
    expect(page.getAttribute('data-ready-endpoints')).toContain('/api/v1/admin/home')
    expect(page).toHaveAttribute('data-bff-endpoint', '/api/v1/admin/home')
    const guard = await screen.findByTestId('admin-knowledge-guard')
    expect(guard).toHaveAttribute('data-official-reference-required', 'true')
    expect(within(guard).getByText('douyin')).toBeInTheDocument()
    expect(within(guard).getByText('douyin_weigui')).toBeInTheDocument()
    expect(roleHomeApi.getHome).toHaveBeenCalledTimes(1)
    expect(roleHomeApi.getHome).toHaveBeenCalledWith('admin')
  })

  it('renders org home with org live and approval contracts', async () => {
    renderRole('org')

    const page = await screen.findByTestId('org-home-page')
    expect(await screen.findByText('机构经营台')).toBeInTheDocument()
    expect(await screen.findByText('待审话术')).toBeInTheDocument()
    await waitFor(() => expect(page).toHaveAttribute('data-owner-boundary', 'owner 7'))
    expect(page).toHaveAttribute('data-shell-path', '/org')
    expect(page.getAttribute('data-ready-endpoints')).toContain('/api/v1/org/home')
    expect(await screen.findByTestId('org-knowledge-guard')).toHaveAttribute('data-block-without-official-reference', 'true')
    expect(await screen.findByTestId('org-business-chains')).toHaveTextContent('直播话术生成/微调')
    expect(await screen.findByTestId('org-learning-loops')).toHaveTextContent('viral_patterns')
    expect(roleHomeApi.getHome).toHaveBeenCalledTimes(1)
    expect(roleHomeApi.getHome).toHaveBeenCalledWith('org')
  })

  it('renders talent home without admin AI distribution calls', async () => {
    renderRole('talent')

    const page = await screen.findByTestId('talent-home-page')
    expect(page).toHaveAttribute('data-shell-path', '/talent')
    expect(page.getAttribute('data-ready-endpoints')).toContain('/api/v1/talent/home')
    expect(await screen.findByText('达人创作台')).toBeInTheDocument()
    expect(await screen.findByText('个人测评短视频')).toBeInTheDocument()
    expect(roleHomeApi.getHome).toHaveBeenCalledTimes(1)
    expect(roleHomeApi.getHome).toHaveBeenCalledWith('talent')
  })

  it('renders user home with only personal shortvideo contracts', async () => {
    renderRole('user')

    const page = await screen.findByTestId('user-home-page')
    expect(page).toHaveAttribute('data-shell-path', '/user')
    expect(page).toHaveAttribute('data-no-cross-role-navigation', 'true')
    expect(page.getAttribute('data-ready-endpoints')).toContain('/api/v1/user/home')
    expect(await screen.findByText('个人创作台')).toBeInTheDocument()
    expect(await screen.findByText('个人测评短视频')).toBeInTheDocument()
    expect(roleHomeApi.getHome).toHaveBeenCalledTimes(1)
    expect(roleHomeApi.getHome).toHaveBeenCalledWith('user')
  })
})
