const SLOT_LABEL_MAP: Record<string, string> = {
  opening: '开场话术',
  product: '产品话术',
  transition: '衔接/转场话术',
  closing: '结尾话术',
  emotional: '情绪话术',
}

export function getStepTitle(label: string): string {
  if (SLOT_LABEL_MAP[label]) return SLOT_LABEL_MAP[label]
  if (label === '连接成功，准备生成') return '准备中'
  const genMatch = label.match(/^生成(.+)中$/)
  if (genMatch) {
    const inner = genMatch[1]
    if (inner === '成篇优化') return '成篇优化'
    if (inner.startsWith('产品')) return `产品话术 ${inner.replace('产品', '')}`
    if (inner.startsWith('转场')) return `衔接话术 ${inner.replace('转场', '')}`
    if (inner === '开场') return '开场话术'
    if (inner === '收尾') return '收尾话术'
    return inner
  }
  if (label.startsWith('产品')) return `产品话术 ${label.replace('产品', '')}`
  if (label.startsWith('转场')) return `衔接话术 ${label.replace('转场', '')}`
  return label
}

export function getStepDescription(label: string): string | undefined {
  if (label === '成篇优化' || label === '生成成篇优化中') return '正在优化全文衔接'
  if (label === '完成') return '全部完成'
  if (label === '连接成功，准备生成') return '正在连接并准备生成环境'
  const genMatch = label.match(/^生成(.+)中$/)
  if (genMatch) {
    const inner = genMatch[1]
    if (inner === '开场') return '正在生成开场话术'
    if (inner === '收尾') return '正在生成收尾话术'
    if (inner === '成篇优化') return '正在优化全文衔接'
    if (inner.startsWith('产品')) return '正在生成产品介绍话术'
    if (inner.startsWith('转场')) return '正在生成衔接话术'
  }
  return undefined
}
