/**
 * 风格权重预设模板
 */

export interface StyleWeightPreset {
  id: string
  name: string
  description: string
  icon?: string
  weights: Record<string, number>
  minStyles: number  // 最少需要几个风格
  maxStyles: number  // 最多支持几个风格
}

/**
 * 预设模板列表
 */
export const STYLE_WEIGHT_PRESETS: StyleWeightPreset[] = [
  {
    id: 'balanced',
    name: '均衡融合',
    description: '所有风格平均分配，适合多风格混合讲解',
    icon: '⚖️',
    weights: {},  // 空对象表示平均分配
    minStyles: 2,
    maxStyles: 10,
  },
  {
    id: 'primary-secondary',
    name: '主次分明',
    description: '主风格70%，次风格30%，适合突出主要风格',
    icon: '🎯',
    weights: { primary: 0.7, secondary: 0.3 },
    minStyles: 2,
    maxStyles: 2,
  },
  {
    id: 'dominant',
    name: '主导风格',
    description: '主风格90%，其他风格平分10%，适合极端风格',
    icon: '👑',
    weights: { primary: 0.9 },
    minStyles: 2,
    maxStyles: 5,
  },
  {
    id: 'progressive',
    name: '渐进过渡',
    description: '第一风格50%，第二风格30%，第三风格20%，适合分段讲解',
    icon: '📊',
    weights: { first: 0.5, second: 0.3, third: 0.2 },
    minStyles: 3,
    maxStyles: 3,
  },
  {
    id: 'pyramid',
    name: '金字塔式',
    description: '第一风格60%，第二风格30%，其他平分10%，适合多风格组合',
    icon: '🔺',
    weights: { first: 0.6, second: 0.3 },
    minStyles: 3,
    maxStyles: 5,
  },
]

/**
 * 应用预设模板到选中的风格
 * @param preset 预设模板
 * @param selectedStyles 选中的风格列表
 * @returns 风格权重映射
 */
export function applyPresetToStyles(
  preset: StyleWeightPreset,
  selectedStyles: string[]
): Record<string, number> {
  const styleCount = selectedStyles.length

  // 检查风格数量是否符合预设要求
  if (styleCount < preset.minStyles || styleCount > preset.maxStyles) {
    return {}
  }

  // 均衡融合：平均分配
  if (preset.id === 'balanced') {
    const weight = 1.0 / styleCount
    return selectedStyles.reduce((acc, style) => {
      acc[style] = weight
      return acc
    }, {} as Record<string, number>)
  }

  // 主次分明：第一个70%，第二个30%
  if (preset.id === 'primary-secondary' && styleCount === 2) {
    return {
      [selectedStyles[0]]: 0.7,
      [selectedStyles[1]]: 0.3,
    }
  }

  // 主导风格：第一个90%，其他平分10%
  if (preset.id === 'dominant' && styleCount >= 2) {
    const remainingWeight = 0.1
    const otherWeight = remainingWeight / (styleCount - 1)
    const result: Record<string, number> = { [selectedStyles[0]]: 0.9 }
    for (let i = 1; i < styleCount; i++) {
      result[selectedStyles[i]] = otherWeight
    }
    return result
  }

  // 渐进过渡：50% + 30% + 20%
  if (preset.id === 'progressive' && styleCount === 3) {
    return {
      [selectedStyles[0]]: 0.5,
      [selectedStyles[1]]: 0.3,
      [selectedStyles[2]]: 0.2,
    }
  }

  // 金字塔式：60% + 30% + 其他平分10%
  if (preset.id === 'pyramid' && styleCount >= 3) {
    const remainingWeight = 0.1
    const otherWeight = remainingWeight / (styleCount - 2)
    const result: Record<string, number> = {
      [selectedStyles[0]]: 0.6,
      [selectedStyles[1]]: 0.3,
    }
    for (let i = 2; i < styleCount; i++) {
      result[selectedStyles[i]] = otherWeight
    }
    return result
  }

  return {}
}

/**
 * 获取适用于当前风格数量的预设模板
 * @param styleCount 选中的风格数量
 * @returns 适用的预设模板列表
 */
export function getApplicablePresets(styleCount: number): StyleWeightPreset[] {
  return STYLE_WEIGHT_PRESETS.filter(
    (preset) => styleCount >= preset.minStyles && styleCount <= preset.maxStyles
  )
}
