/**
 * 话术管理共享常量
 * ProductScriptManageDialog / BatchScriptGenerateDialog 共用
 */

/** 话术类型选项 */
export const SCRIPT_TYPE_OPTIONS = [
  { value: 'seed', label: '种草' },
  { value: 'promotion', label: '促销' },
  { value: 'formal', label: '正式' },
]

/** 风格预设 fallback 标签（presetCode → 中文）— 与后端 ALLOWED_STYLES 对齐（16 种） */
export const FALLBACK_STYLE_LABEL: Record<string, string> = {
  professional: '专业版',
  friendly: '亲切版',
  passionate: '激情版',
  seeding: '种草版',
  promotion: '促销版',
  chicken_soup: '鸡汤版',
  proverb: '金句版',
  heart_piercing: '扎心版',
  humorous: '幽默版',
  emotional_intelligence: '高情商版',
  persona_flavor: '人设风味版',
  local_flavor: '接地气版',
  creative: '创意版',
  enthusiastic: '热情版',
  casual: '随性版',
  warm: '暖心版',
}

/**
 * 风格预设 fallback 列表（后端未加载到 StylePreset 时使用）
 * 前 5 个为核心风格，后 11 个为扩展风格
 */
export const FALLBACK_STYLE_PRESETS = [
  // ── 核心风格 ──
  { presetCode: 'professional', presetName: '专业版', description: '专业严谨，逻辑清晰', category: 'core' },
  { presetCode: 'friendly', presetName: '亲切版', description: '温暖亲切，像朋友推荐', category: 'core' },
  { presetCode: 'passionate', presetName: '激情版', description: '热情洋溢，感染力强', category: 'core' },
  { presetCode: 'seeding', presetName: '种草版', description: '真实体验，强调感受', category: 'core' },
  { presetCode: 'promotion', presetName: '促销版', description: '突出优惠，营造紧迫感', category: 'core' },
  // ── 扩展风格 ──
  { presetCode: 'chicken_soup', presetName: '鸡汤版', description: '走心语录，引发共鸣', category: 'extended' },
  { presetCode: 'proverb', presetName: '金句版', description: '朗朗上口，记忆深刻', category: 'extended' },
  { presetCode: 'heart_piercing', presetName: '扎心版', description: '直击痛点，引发焦虑', category: 'extended' },
  { presetCode: 'humorous', presetName: '幽默版', description: '轻松风趣，拉近距离', category: 'extended' },
  { presetCode: 'emotional_intelligence', presetName: '高情商版', description: '情绪共鸣，润物无声', category: 'extended' },
  { presetCode: 'persona_flavor', presetName: '人设风味版', description: '个性鲜明，强化人设', category: 'extended' },
  { presetCode: 'local_flavor', presetName: '接地气版', description: '口语化表达，贴近生活', category: 'extended' },
  { presetCode: 'creative', presetName: '创意版', description: '打破常规，出奇制胜', category: 'extended' },
  { presetCode: 'enthusiastic', presetName: '热情版', description: '充满活力，带动气氛', category: 'extended' },
  { presetCode: 'casual', presetName: '随性版', description: '随意自然，不刻意推销', category: 'extended' },
  { presetCode: 'warm', presetName: '暖心版', description: '温暖关怀，传递善意', category: 'extended' },
] as const

/** 话术时长选项（秒） */
export const DURATION_OPTIONS = [
  { value: 30, label: '30 秒' },
  { value: 60, label: '60 秒' },
  { value: 90, label: '90 秒' },
  { value: 120, label: '120 秒' },
]

/** 直播场景选项 — 与后端 ALLOWED_SCENES 对齐 */
export const SCENE_OPTIONS = [
  { value: '', label: '通用（不限场景）' },
  { value: 'danpin', label: '单品讲解' },
  { value: 'guopin', label: '过品快讲' },
  { value: 'cangbo', label: '仓播专场' },
  { value: 'short_video', label: '短视频带货' },
  { value: 'yubo', label: '娱播插品' },
]
