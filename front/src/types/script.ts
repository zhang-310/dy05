/**
 * 话术库模块 - TypeScript 类型定义
 * 与后端 module/script/ VO 一一对应
 */

import type { BasicQuery } from './common'

/** 话术库查询参数（对应后端 ScriptSearchVO） */
export interface ScriptSearchVO extends BasicQuery {
  keyword?: string
  category?: string
  source?: string
  status?: number
  userId?: number
  userIds?: number[]
}

/** 话术库保存参数（对应后端 ScriptSaveVO） */
export interface ScriptSaveVO {
  id?: number
  userId?: number
  title: string
  content?: string
  category?: string
  source?: string
  sourceId?: number
  tags?: string
  status?: number
}

/** 话术库返回值（对应后端 ScriptVO） */
export interface ScriptVO {
  id: number
  userId: number
  title: string
  content?: string
  category?: string
  source?: string
  sourceId?: number
  tags?: string
  useCount: number
  status: number
  createTime: string
  updateTime: string
}

/** 违规词查询参数 */
export interface ViolationWordSearchVO extends BasicQuery {
  keyword?: string
  level?: number
  source?: string
}

/** 违规词保存参数 */
export interface ViolationWordSaveVO {
  id?: number
  word: string
  reason?: string
  level?: number
  replacement?: string
}

/** 违规词返回值 */
export interface ViolationWordVO {
  id: number
  word: string
  reason?: string
  level?: number
  replacement?: string
  source?: string
  createTime?: string
  updateTime?: string
}

/** 话术模板查询参数 */
export interface TemplateSearchVO extends BasicQuery {
  keyword?: string
  category?: string
  scriptType?: string
}

/** 话术模板保存参数 */
export interface TemplateSaveVO {
  id?: number
  templateName: string
  content: string
  category?: string
  scriptType?: string
}

/** 话术模板返回值 */
export interface TemplateVO {
  id: number
  templateName: string
  content: string
  category?: string
  scriptType?: string
  useCount?: number
  createTime?: string
  updateTime?: string
}
