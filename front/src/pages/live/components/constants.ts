/** 直播话术构建页共享常量 */

export const PRODUCT_TYPE_OPTIONS = [
  { value: 'hot',     label: '爆品',   color: 'error'   as const, hint: '深度讲解 2-5 分钟' },
  { value: 'control', label: '控单品', color: 'warning' as const, hint: '控单憋单 90-150 秒' },
  { value: 'profit',  label: '利润品', color: 'success' as const, hint: '重点推介 60-90 秒' },
  { value: 'loss',    label: '亏品',   color: 'info'   as const, hint: '快速过品 30-60 秒' },
  { value: 'flat',    label: '平价品', color: 'default' as const, hint: '标准话术 45-75 秒' },
]

export const PRODUCT_TYPE_COLOR_MAP: Record<string, string> = {
  hot: '#ef5350',
  control: '#ff9800',
  profit: '#66bb6a',
  loss: '#42a5f5',
  flat: '#9e9e9e',
}

export const PRODUCT_TYPE_DURATION_MAP: Record<string, number> = {
  hot: 180,
  control: 120,
  profit: 90,
  loss: 60,
  flat: 60,
}

export const SCRIPT_TYPE_LABEL: Record<string, string> = {
  opening: '开场',
  product: '产品',
  transition: '转场',
  closing: '结尾',
  emotional: '情绪话术',
  custom: '自定义',
}

/** 风格分组（用于槽位风格菜单） */
export const SCRIPT_STYLE_GROUPS = [
  {
    label: '基础风格',
    styles: [
      { value: 'professional', label: '专业' },
      { value: 'friendly', label: '亲切' },
      { value: 'passionate', label: '热情' },
      { value: 'seeding', label: '种草' },
      { value: 'promotion', label: '促销' },
      { value: 'natural', label: '自然' },
    ],
  },
  {
    label: '互动风格',
    styles: [
      { value: 'interactive', label: '强互动' },
      { value: 'question', label: '问答式' },
      { value: 'storytelling', label: '故事式' },
      { value: 'emotional', label: '情感共鸣' },
    ],
  },
  {
    label: '促单风格',
    styles: [
      { value: 'urgency', label: '紧迫感' },
      { value: 'scarcity', label: '稀缺性' },
      { value: 'comparison', label: '对比式' },
      { value: 'authority', label: '权威背书' },
    ],
  },
]

/** 时长分配模式 */
export const DURATION_MODES: Record<string, { label: string; desc: string; values: Record<string, number> }> = {
  compact: {
    label: '紧凑',
    desc: '快速过品，每款 60-90 秒',
    values: { hot: 90, profit: 60, loss: 45, flat: 45, control: 90, opening: 30, transition: 15, closing: 30 },
  },
  standard: {
    label: '标准',
    desc: '均衡分配，每款 90-180 秒',
    values: { hot: 180, profit: 90, loss: 60, flat: 60, control: 120, opening: 45, transition: 30, closing: 45 },
  },
  relaxed: {
    label: '宽松',
    desc: '深度讲解，每款 180-300 秒',
    values: { hot: 300, profit: 180, loss: 90, flat: 90, control: 180, opening: 60, transition: 45, closing: 60 },
  },
  type: {
    label: '按品类',
    desc: '根据商品品类自动推荐时长',
    values: { hot: 180, profit: 90, loss: 60, flat: 60, control: 120, opening: 45, transition: 30, closing: 45 },
  },
}

export const SCRIPT_STYLE_OPTIONS = [
  { value: '', label: '默认' },
  { value: 'professional', label: '专业' },
  { value: 'friendly', label: '亲切' },
  { value: 'passionate', label: '热情' },
  { value: 'seeding', label: '种草' },
  { value: 'promotion', label: '促销' },
]

export const REQUIREMENT_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '无特殊要求' },
  { value: 'highlight_pain', label: '突出痛点' },
  { value: 'highlight_benefit', label: '强调功效' },
  { value: 'highlight_price', label: '强调价格' },
  { value: 'highlight_scarcity', label: '制造紧迫感' },
  { value: 'story_driven', label: '故事性叙述' },
  { value: 'comparison', label: '对比竞品' },
  { value: 'user_review', label: '用户评价角度' },
]

export const EMOTIONAL_CATEGORIES: {
  value: string
  label: string
  subOptions?: { value: string; label: string }[]
}[] = [
  {
    value: 'urgency',
    label: '紧迫感',
    subOptions: [
      { value: 'time_limit', label: '限时' },
      { value: 'stock_limit', label: '限量' },
      { value: 'price_rise', label: '即将涨价' },
    ],
  },
  {
    value: 'trust',
    label: '信任背书',
    subOptions: [
      { value: 'authority', label: '权威认证' },
      { value: 'user_review', label: '用户口碑' },
      { value: 'celebrity', label: '明星推荐' },
    ],
  },
  {
    value: 'resonance',
    label: '情感共鸣',
    subOptions: [
      { value: 'pain_point', label: '痛点共鸣' },
      { value: 'aspiration', label: '向往激励' },
      { value: 'belonging', label: '归属感' },
    ],
  },
  {
    value: 'curiosity',
    label: '好奇引导',
    subOptions: [
      { value: 'mystery', label: '悬念' },
      { value: 'surprise', label: '反转惊喜' },
      { value: 'reveal', label: '揭秘' },
    ],
  },
  {
    value: 'fomo',
    label: 'FOMO',
    subOptions: [
      { value: 'exclusive', label: '专属特权' },
      { value: 'trending', label: '热门跟风' },
    ],
  },
]

export const REFINE_SUGGESTIONS = ['改成30秒以内', '换成促销风格', '改成促单话术', '缩短一点', '更亲切一些']

export const DIMENSION_OPTIONS = ['拉停留', '互动', '促单', '种草', '转场', '引流', '成交']

export function parseProductTypes(raw: unknown): string[] {
  if (!raw) return []
  const s = String(raw)
  return s.split(',').map(t => t.trim()).filter(Boolean)
}

export function formatProductTypes(types: string[]): string {
  return types.filter(Boolean).join(',')
}

/** 智能时长分配：根据脚本类型和商品类型推荐时长 */
export function smartAllocateDurations({
  scripts,
  totalTargetSec,
  mode = 'standard',
}: {
  scripts: Array<{ id: number; scriptType?: string; productType?: string }>
  totalTargetSec: number
  mode?: string
}): Map<number, number> {
  const result = new Map<number, number>()
  const modeValues = DURATION_MODES[mode]?.values ?? DURATION_MODES.standard.values
  const defaultPerSlot = Math.round(totalTargetSec / Math.max(scripts.length, 1))

  for (const s of scripts) {
    const type = s.scriptType ?? 'product'
    const ptype = s.productType ? parseProductTypes(s.productType)[0] : undefined
    let dur = modeValues[type] ?? (ptype ? PRODUCT_TYPE_DURATION_MAP[ptype] : undefined) ?? defaultPerSlot
    dur = Math.max(30, Math.min(600, dur))
    result.set(s.id, dur)
  }
  return result
}
