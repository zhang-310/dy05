/**
 * 测试数据工厂
 * 提供创建测试数据的工厂函数
 */

import type {
  LiveSession,
  Script,
  Product,
  KnowledgeBase,
  User,
  Agent,
  ShortVideo,
  Copy,
} from '@/types'

/**
 * 创建测试用户
 */
export function createMockUser(overrides?: Partial<User>): User {
  return {
    id: 1,
    username: 'testuser',
    nickname: '测试用户',
    email: 'test@example.com',
    phone: '13800138000',
    avatar: '',
    roles: ['USER'],
    status: 1,
    createTime: '2024-01-01 00:00:00',
    updateTime: '2024-01-01 00:00:00',
    ...overrides,
  }
}

/**
 * 创建测试直播场次
 */
export function createMockLiveSession(overrides?: Partial<LiveSession>): LiveSession {
  return {
    id: 1,
    title: '测试直播场次',
    description: '这是一个测试直播场次',
    status: 'draft',
    startTime: '2024-01-01 10:00:00',
    endTime: '2024-01-01 12:00:00',
    platform: 'douyin',
    accountId: 1,
    ownerId: 1,
    createTime: '2024-01-01 00:00:00',
    updateTime: '2024-01-01 00:00:00',
    ...overrides,
  }
}

/**
 * 创建测试话术
 */
export function createMockScript(overrides?: Partial<Script>): Script {
  return {
    id: 1,
    title: '测试话术',
    content: '这是一段测试话术内容',
    type: 'opening',
    status: 'draft',
    liveSessionId: 1,
    productId: null,
    ownerId: 1,
    tags: ['测试'],
    version: 1,
    createTime: '2024-01-01 00:00:00',
    updateTime: '2024-01-01 00:00:00',
    ...overrides,
  }
}

/**
 * 创建测试商品
 */
export function createMockProduct(overrides?: Partial<Product>): Product {
  return {
    id: 1,
    name: '测试商品',
    description: '这是一个测试商品',
    price: 99.99,
    originalPrice: 199.99,
    stock: 100,
    imageUrl: 'https://example.com/product.jpg',
    category: '护肤品',
    brand: '测试品牌',
    status: 'active',
    ownerId: 1,
    createTime: '2024-01-01 00:00:00',
    updateTime: '2024-01-01 00:00:00',
    ...overrides,
  }
}

/**
 * 创建测试知识库
 */
export function createMockKnowledgeBase(overrides?: Partial<KnowledgeBase>): KnowledgeBase {
  return {
    id: 1,
    name: '测试知识库',
    description: '这是一个测试知识库',
    type: 'product',
    status: 'active',
    documentCount: 10,
    ownerId: 1,
    createTime: '2024-01-01 00:00:00',
    updateTime: '2024-01-01 00:00:00',
    ...overrides,
  }
}

/**
 * 创建测试智能体
 */
export function createMockAgent(overrides?: Partial<Agent>): Agent {
  return {
    id: 1,
    name: '测试智能体',
    description: '这是一个测试智能体',
    type: 'chat',
    status: 'active',
    config: {},
    ownerId: 1,
    createTime: '2024-01-01 00:00:00',
    updateTime: '2024-01-01 00:00:00',
    ...overrides,
  }
}

/**
 * 创建测试短视频
 */
export function createMockShortVideo(overrides?: Partial<ShortVideo>): ShortVideo {
  return {
    id: 1,
    title: '测试短视频',
    description: '这是一个测试短视频',
    status: 'draft',
    duration: 60,
    videoUrl: '',
    coverUrl: '',
    platform: 'douyin',
    accountId: 1,
    ownerId: 1,
    createTime: '2024-01-01 00:00:00',
    updateTime: '2024-01-01 00:00:00',
    ...overrides,
  }
}

/**
 * 创建测试文案
 */
export function createMockCopy(overrides?: Partial<Copy>): Copy {
  return {
    id: 1,
    title: '测试文案',
    content: '这是一段测试文案内容',
    type: 'product',
    status: 'active',
    tags: ['测试'],
    ownerId: 1,
    createTime: '2024-01-01 00:00:00',
    updateTime: '2024-01-01 00:00:00',
    ...overrides,
  }
}

/**
 * 创建分页响应
 */
export function createMockPageResponse<T>(
  data: T[],
  total?: number,
  pageNum = 1,
  pageSize = 30
) {
  return {
    list: data,
    total: total ?? data.length,
    pageNum,
    pageSize,
  }
}

/**
 * 创建 API 成功响应
 */
export function createMockApiResponse<T>(data: T) {
  return {
    status: 200,
    message: '操作成功',
    data,
    traceId: 'test-trace-id',
    timestamp: Date.now(),
  }
}

/**
 * 创建 API 错误响应
 */
export function createMockApiError(message = '操作失败', status = 500) {
  return {
    status,
    message,
    data: null,
    traceId: 'test-trace-id',
    timestamp: Date.now(),
  }
}
