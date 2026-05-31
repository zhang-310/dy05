import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/utils/request'
import { roleHomeApi } from '../role-home'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('roleHomeApi', () => {
  const mockPost = vi.mocked(request.post)

  beforeEach(() => {
    mockPost.mockReset()
  })

  it.each([
    ['admin', '/admin/home'],
    ['org', '/org/home'],
    ['talent', '/talent/home'],
    ['user', '/user/home'],
  ] as const)('loads %s role home from its BFF endpoint', async (role, endpoint) => {
    mockPost.mockResolvedValue({
      role,
      userId: '7',
      roleCode: role === 'org' ? 'institution' : role,
      organizationId: 88,
      visibleOwnerIds: ['7', 9],
      boundary: {
        ownerId: 7,
        organizationId: 88,
        unrestricted: false,
        visibleOwnerIds: ['7', 9],
        requiresOwnerFilter: true,
      },
      metrics: { projectCount: '2' },
      knowledgeGuard: {
        requiredKbCodes: ['douyin', 'douyin_weigui'],
        officialReferenceRequired: true,
        blockWithoutOfficialReference: role !== 'admin',
        appliesTo: ['shortvideo-script'],
      },
      businessChains: [
        { key: 'shortvideo-script', title: '短视频脚本', path: '/talent/shortvideo', requiredKnowledge: 'douyin,douyin_weigui', guardRequired: true },
      ],
      learningLoops: [
        { key: 'viral-pattern-kb', title: '爆款采集结果结构化入库', targetKb: 'viral_patterns', source: 'benchmark_collect_result', status: 'contract-ready' },
      ],
      readyEndpoints: [`/api/v1/${role}/home`],
      sections: [
        { key: 'recentProjects', title: 'Recent Projects', items: [{ title: '项目 A' }] },
      ],
    })

    const result = await roleHomeApi.getHome(role)

    expect(mockPost).toHaveBeenCalledWith(endpoint, {})
    expect(result).toMatchObject({
      role,
      roleCode: role === 'org' ? 'institution' : role,
      organizationId: 88,
      visibleOwnerIds: [7, 9],
      boundary: {
        organizationId: 88,
        unrestricted: false,
        visibleOwnerIds: [7, 9],
        requiresOwnerFilter: true,
      },
      metrics: { projectCount: '2' },
      knowledgeGuard: {
        requiredKbCodes: ['douyin', 'douyin_weigui'],
        officialReferenceRequired: true,
        blockWithoutOfficialReference: role !== 'admin',
        appliesTo: ['shortvideo-script'],
      },
      businessChains: [
        { key: 'shortvideo-script', title: '短视频脚本', path: '/talent/shortvideo', requiredKnowledge: 'douyin,douyin_weigui', guardRequired: true },
      ],
      learningLoops: [
        { key: 'viral-pattern-kb', title: '爆款采集结果结构化入库', targetKb: 'viral_patterns', source: 'benchmark_collect_result', status: 'contract-ready' },
      ],
      readyEndpoints: [`/api/v1/${role}/home`],
      sections: [
        { key: 'recentProjects', title: 'Recent Projects', items: [{ title: '项目 A' }] },
      ],
    })
  })

  it('keeps admin unrestricted owner boundary as null instead of an empty list', async () => {
    mockPost.mockResolvedValue({
      role: 'admin',
      visibleOwnerIds: null,
      boundary: {
        unrestricted: true,
        visibleOwnerIds: null,
      },
      metrics: {},
      readyEndpoints: ['/api/v1/admin/home'],
      sections: [],
    })

    const result = await roleHomeApi.getHome('admin')

    expect(result.visibleOwnerIds).toBeNull()
    expect(result.boundary?.unrestricted).toBe(true)
    expect(result.boundary?.visibleOwnerIds).toBeNull()
  })
})
