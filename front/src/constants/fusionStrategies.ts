/**
 * 风格融合策略
 */

export interface FusionStrategy {
  id: string
  name: string
  description: string
  icon?: string
  minStyles: number  // 最少需要几个风格
  maxStyles: number  // 最多支持几个风格
  requiresWeights: boolean  // 是否需要权重配置
}

/**
 * 融合策略列表
 */
export const FUSION_STRATEGIES: FusionStrategy[] = [
  {
    id: 'blended',
    name: '混合融合',
    description: '将多个风格自然混合，整体协调统一',
    icon: '🎨',
    minStyles: 2,
    maxStyles: 10,
    requiresWeights: true,
  },
  {
    id: 'sequential',
    name: '顺序融合',
    description: '按风格顺序依次呈现，适合分段讲解',
    icon: '📝',
    minStyles: 2,
    maxStyles: 5,
    requiresWeights: true,
  },
  {
    id: 'layered',
    name: '分层融合',
    description: '主风格为基础，其他风格作为点缀',
    icon: '🎯',
    minStyles: 2,
    maxStyles: 5,
    requiresWeights: true,
  },
  {
    id: 'alternating',
    name: '交替融合',
    description: '多个风格交替出现，节奏感强',
    icon: '🔄',
    minStyles: 2,
    maxStyles: 4,
    requiresWeights: false,
  },
  {
    id: 'progressive',
    name: '渐进融合',
    description: '从第一风格逐渐过渡到最后风格',
    icon: '📊',
    minStyles: 2,
    maxStyles: 5,
    requiresWeights: false,
  },
]

/**
 * 获取适用于当前风格数量的融合策略
 * @param styleCount 选中的风格数量
 * @returns 适用的融合策略列表
 */
export function getApplicableFusionStrategies(styleCount: number): FusionStrategy[] {
  return FUSION_STRATEGIES.filter(
    (strategy) => styleCount >= strategy.minStyles && styleCount <= strategy.maxStyles
  )
}

/**
 * 生成融合策略的提示词
 * @param strategy 融合策略
 * @param styles 风格列表
 * @param styleWeights 风格权重（可选）
 * @returns 融合策略提示词
 */
export function generateFusionStrategyPrompt(
  strategy: FusionStrategy,
  styles: string[],
  styleWeights?: Record<string, number>
): string {
  const styleCount = styles.length

  switch (strategy.id) {
    case 'blended':
      // 混合融合：自然混合，整体协调
      if (styleWeights && Object.keys(styleWeights).length > 0) {
        const weightInfo = styles
          .map((style) => {
            const weight = styleWeights[style] || 1.0 / styleCount
            return `${style}(${Math.round(weight * 100)}%)`
          })
          .join('、')
        return `将以下风格自然混合为一条连贯话术，保持整体协调统一。风格权重：${weightInfo}。确保各风格特点自然融入，避免生硬切换。`
      }
      return `将 ${styles.join('、')} 等风格自然混合为一条连贯话术，保持整体协调统一。`

    case 'sequential':
      // 顺序融合：按顺序依次呈现
      if (styleWeights && Object.keys(styleWeights).length > 0) {
        const segments = styles.map((style, index) => {
          const weight = styleWeights[style] || 1.0 / styleCount
          const percentage = Math.round(weight * 100)
          return `第${index + 1}段：${style}风格（占比${percentage}%）`
        })
        return `按以下顺序分段呈现话术：\n${segments.join('\n')}\n每段风格清晰，过渡自然流畅。`
      }
      return `按 ${styles.join(' → ')} 的顺序分段呈现话术，每段风格清晰，过渡自然流畅。`

    case 'layered':
      // 分层融合：主风格为基础，其他风格点缀
      const primaryStyle = styles[0]
      const accentStyles = styles.slice(1)
      return `以 ${primaryStyle} 风格为主基调，在关键位置点缀 ${accentStyles.join('、')} 等风格元素。主风格贯穿全文，其他风格作为亮点出现。`

    case 'alternating':
      // 交替融合：多个风格交替出现
      return `让 ${styles.join('、')} 等风格交替出现，形成节奏感。每个风格片段简短有力，切换自然不突兀。`

    case 'progressive':
      // 渐进融合：从第一风格逐渐过渡到最后风格
      return `从 ${styles[0]} 风格开始，逐渐过渡到 ${styles[styles.length - 1]} 风格。中间经过 ${styles.slice(1, -1).join('、')} 等风格的自然过渡，整体呈现渐进变化。`

    default:
      return `将 ${styles.join('、')} 等风格融合为一条连贯话术。`
  }
}

/**
 * 检查策略是否需要权重配置
 * @param strategyId 策略ID
 * @returns 是否需要权重配置
 */
export function strategyRequiresWeights(strategyId: string): boolean {
  const strategy = FUSION_STRATEGIES.find((s) => s.id === strategyId)
  return strategy?.requiresWeights ?? false
}
