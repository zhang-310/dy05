/**
 * 话术库分类常量
 * 与后端 ChunkLabeler.java 保持一致
 */

export const SCRIPT_TYPE_CATEGORIES = [
  '种草',
  '促销',
  '产品介绍',
  '直播话术',
  '情绪价值',
  '过渡话术',
  '互动话术',
  'FAQ',
] as const

export const SCRIPT_CAT_CATEGORIES = [
  '美妆护肤',
  '食品',
  '服装',
  '家居',
  '数码',
] as const

export const ALL_SCRIPT_CATEGORIES = [
  ...SCRIPT_TYPE_CATEGORIES,
  ...SCRIPT_CAT_CATEGORIES,
] as const

export type ScriptCategory = typeof ALL_SCRIPT_CATEGORIES[number]
