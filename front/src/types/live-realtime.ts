/**
 * 直播实时辅助面板的 TypeScript 类型定义
 * 与后端 VO 对应
 */

/**
 * 直播话术段落值对象
 * 用于显示话术段落的详细信息
 */
export interface LiveSessionScriptSlotVO {
  /** 话术段落 ID */
  id: number
  /** 直播场次 ID */
  liveSessionId: number
  /** 段落序号（从 0 开始）*/
  slotIndex: number
  /** 话术版本 ID */
  scriptVersionId?: number
  /** 话术内容 */
  content: string
  /** 建议讲解时长（秒）*/
  durationSeconds: number
  /** 话术类型（opening/product/discount/closing/emotional）*/
  scriptType?: string
  /** 话术风格（enthusiastic/professional/gentle/humorous）*/
  style?: string
  /** 是否当前段落 */
  isCurrent: boolean
  /** 是否已讲解完成 */
  isCompleted: boolean
  /** 开始讲解时间 */
  startedAt?: string
  /** 完成讲解时间 */
  completedAt?: string
}

/**
 * 直播实时数据值对象
 * 用于显示实时统计数据
 */
export interface LiveSessionRealtimeDataVO {
  /** 实时数据 ID */
  id: number
  /** 直播场次 ID */
  liveSessionId: number
  /** 累计观看人数 */
  watchedCount: number
  /** 在线观众数 */
  viewerCount: number
  /** 点赞总数 */
  likeCount: number
  /** 评论总数 */
  commentCount: number
  /** 分享总数 */
  shareCount: number
  /** 新增关注数 */
  followCount: number
  /** 礼物金额总额（元）*/
  giftAmount: number | string
  /** 产品点击数 */
  productClickCount: number
  /** 商品购买数 */
  productPurchaseCount: number
  /** 商品购买总金额（元）*/
  productPurchaseAmount: number | string
  /** 当前话术段落序号 */
  currentSlotIndex?: number
}

/**
 * 直播实时面板初始化响应
 * 包含所有话术段落和实时数据
 */
export interface PanelInitVO {
  /** 直播场次 ID */
  liveSessionId: number
  /** 所有话术段落列表 */
  slots: LiveSessionScriptSlotVO[]
  /** 当前话术段落序号 */
  currentSlotIndex: number
  /** 本场直播目标 GMV（后端提供时展示目标进度） */
  targetGmv?: number | string
  /** 兼容历史或聚合接口里的目标 GMV 字段名 */
  gmvTarget?: number | string
  /** 兼容目标销售额字段名 */
  targetSalesAmount?: number | string
  /** 实时数据 */
  realtimeData: LiveSessionRealtimeDataVO
}

/**
 * 话术段落操作参数
 * 用于前端发送话术操作请求
 */
export interface SlotOperationVO {
  /** 直播场次 ID */
  liveSessionId: number
  /** 目标段落序号（跳转时使用）*/
  slotIndex?: number
}

/**
 * 直播实时数据保存参数
 * 用于保存或更新实时数据
 */
export interface RealtimeDataSaveVO {
  /** 直播场次 ID */
  liveSessionId: number
  /** 观看人数 */
  watchedCount?: number
  /** 在线人数 */
  viewerCount?: number
  /** 点赞数 */
  likeCount?: number
  /** 评论数 */
  commentCount?: number
  /** 分享数 */
  shareCount?: number
  /** 关注数 */
  followCount?: number
  /** 礼物金额（元）*/
  giftAmount?: number | string
  /** 产品点击数 */
  productClickCount?: number
  /** 商品购买数 */
  productPurchaseCount?: number
  /** 商品购买金额（元）*/
  productPurchaseAmount?: number | string
}

/**
 * 标准分页结果
 * 用于分页查询响应
 */
export interface PageResult<T> {
  /** 总记录数 */
  total: number
  /** 数据列表 */
  list: T[]
  /** 当前页码 */
  pageNum: number
  /** 页面大小 */
  pageSize: number
}

/**
 * SSE 事件类型
 */
export type SSEEventType = 'data-update' | 'slot-change' | 'slot-completed' | 'connected' | 'error'

/**
 * SSE 实时数据更新事件
 */
export interface SSEDataUpdateEvent {
  type: 'realtime-data'
  likeCount: number
  commentCount: number
  viewerCount: number
  watchedCount: number
  timestamp: string | number
}

/**
 * SSE 话术变化事件
 */
export interface SSESlotChangeEvent {
  type: 'slot-changed'
  currentSlotIndex: number
  content: string
  durationSeconds: number
  startedAt: string
}

/**
 * SSE 话术完成事件
 */
export interface SSESlotCompletedEvent {
  type: 'slot-completed'
  completedSlotIndex: number
  completedAt: string | number
}
