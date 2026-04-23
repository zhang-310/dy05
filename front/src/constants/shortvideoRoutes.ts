/**
 * Admin 短视频完整路径（与 `router/index.tsx` 中 `path: 'shortvideo/...'` 子路径一致）。
 *
 * 页面能打开但功能异常时：在浏览器 Network 中查看 API 状态码与响应体；
 * 404/501 多为后端未实现；5xx 查服务端日志。OpenAPI/接口文档与 Controller 对照可快速定位模块缺口。
 *
 * ## 洞见与拆解（产品主线）
 * - 采集入库：`collect` → 账号资产：`accounts` / `shortvideoAccountDetailPath(id,'videos')`
 * - 爆款库 + LF 深度拆解：`viralVideos`，详情深链：`${viralVideos}?videoId=<库内主键>`
 * - 进化引擎「爆款分析」任务页在 AI 模块：`ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION`；短视频域别名 `viralAnalysisEvolution` 由 router 重定向到前者。
 */
export const ADMIN_SHORTVIDEO_BASE = '/admin/shortvideo' as const

/** 进化引擎爆款分析（ViralAnalysisPage），与短视频爆款库 LF 拆解区分 */
export const ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION = '/admin/ai/viral-analysis' as const

export const shortvideoRoutes = {
  /** 洞见入口：账号库 / 爆款库 / 进化分析 / 人设融合 */
  insights: `${ADMIN_SHORTVIDEO_BASE}/insights`,
  /** 与 ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION 同页，router 内 Navigate 重定向 */
  viralAnalysisEvolution: `${ADMIN_SHORTVIDEO_BASE}/viral-analysis`,
  projects: `${ADMIN_SHORTVIDEO_BASE}/projects`,
  collect: `${ADMIN_SHORTVIDEO_BASE}/collect`,
  accounts: `${ADMIN_SHORTVIDEO_BASE}/accounts`,
  /** 抖音 Web Cookie（Playwright/采集风控时建议配置） */
  douyinCookies: `${ADMIN_SHORTVIDEO_BASE}/douyin-cookies`,
  remakeTemplates: `${ADMIN_SHORTVIDEO_BASE}/remake-templates`,
  viralVideos: `${ADMIN_SHORTVIDEO_BASE}/viral-videos`,
  shotList: `${ADMIN_SHORTVIDEO_BASE}/shot-list`,
  material: `${ADMIN_SHORTVIDEO_BASE}/material`,
  editing: `${ADMIN_SHORTVIDEO_BASE}/editing`,
  publish: `${ADMIN_SHORTVIDEO_BASE}/publish`,
  quality: `${ADMIN_SHORTVIDEO_BASE}/quality`,
  daily: `${ADMIN_SHORTVIDEO_BASE}/daily`,
  viralChain: `${ADMIN_SHORTVIDEO_BASE}/viral-chain`,
  hotTopics: `${ADMIN_SHORTVIDEO_BASE}/hot-topics`,
  hotTopicCreate: `${ADMIN_SHORTVIDEO_BASE}/hot-topics/create`,
  contentCalendar: `${ADMIN_SHORTVIDEO_BASE}/content-calendar`,
  drama: `${ADMIN_SHORTVIDEO_BASE}/drama`,
  competitorMonitor: `${ADMIN_SHORTVIDEO_BASE}/competitor-monitor`,
  effectPredict: `${ADMIN_SHORTVIDEO_BASE}/effect-predict`,
  dataAnalysis: `${ADMIN_SHORTVIDEO_BASE}/data-analysis`,
  dashboard: `${ADMIN_SHORTVIDEO_BASE}/dashboard`,
  quickGenerate: `${ADMIN_SHORTVIDEO_BASE}/quick-generate`,
  scriptPlanning: `${ADMIN_SHORTVIDEO_BASE}/script-planning`,
  materialPrepare: `${ADMIN_SHORTVIDEO_BASE}/material-prepare`,
  materialProduction: `${ADMIN_SHORTVIDEO_BASE}/material-production`,
  workbench: `${ADMIN_SHORTVIDEO_BASE}/workbench`,
  workflowEditor: `${ADMIN_SHORTVIDEO_BASE}/workflow-editor`,
  personaFusion: `${ADMIN_SHORTVIDEO_BASE}/persona-fusion`,
  aiMusic: `${ADMIN_SHORTVIDEO_BASE}/ai-music`,
  seoOptimize: `${ADMIN_SHORTVIDEO_BASE}/seo-optimize`,
} as const

export type ShortvideoRouteKey = keyof typeof shortvideoRoutes

export function shortvideoSubtitlePath(id: string | number): string {
  return `${ADMIN_SHORTVIDEO_BASE}/subtitle-editor/${id}`
}

/** 账号详情子页：videos=采集视频列表，analysis=基于该账号全部采集视频的综合分析 */
export function shortvideoAccountDetailPath(
  id: string | number,
  tab?: 'info' | 'videos' | 'analysis'
): string {
  const base = `${ADMIN_SHORTVIDEO_BASE}/accounts/${id}`
  if (tab === 'videos' || tab === 'analysis') {
    return `${base}?tab=${tab}`
  }
  return base
}

/** 冒烟/回归：与 router 注册的列表页路径一致（字幕页使用占位 id） */
export const SHORTVIDEO_SMOKE_ROUTE_PATHS: readonly string[] = [
  shortvideoRoutes.dashboard,
  shortvideoRoutes.insights,
  shortvideoRoutes.viralAnalysisEvolution,
  shortvideoRoutes.quickGenerate,
  shortvideoRoutes.projects,
  shortvideoRoutes.workbench,
  shortvideoRoutes.scriptPlanning,
  shortvideoRoutes.shotList,
  shortvideoRoutes.material,
  shortvideoRoutes.materialPrepare,
  shortvideoRoutes.materialProduction,
  shortvideoRoutes.editing,
  shortvideoRoutes.publish,
  shortvideoRoutes.contentCalendar,
  shortvideoRoutes.daily,
  shortvideoRoutes.hotTopics,
  shortvideoRoutes.hotTopicCreate,
  shortvideoRoutes.collect,
  shortvideoRoutes.accounts,
  shortvideoRoutes.douyinCookies,
  shortvideoRoutes.remakeTemplates,
  shortvideoRoutes.viralVideos,
  shortvideoRoutes.drama,
  shortvideoRoutes.quality,
  shortvideoRoutes.viralChain,
  shortvideoRoutes.competitorMonitor,
  shortvideoRoutes.effectPredict,
  shortvideoRoutes.dataAnalysis,
  shortvideoRoutes.workflowEditor,
  shortvideoSubtitlePath(0),
  shortvideoRoutes.personaFusion,
  shortvideoRoutes.aiMusic,
  shortvideoRoutes.seoOptimize,
]
