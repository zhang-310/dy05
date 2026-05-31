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
export const TALENT_SHORTVIDEO_BASE = '/talent/shortvideo' as const
export const USER_SHORTVIDEO_BASE = '/user/shortvideo' as const

/** 进化引擎爆款分析（ViralAnalysisPage），与短视频爆款库 LF 拆解区分 */
export const ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION = '/admin/ai/viral-analysis' as const

const SHORTVIDEO_BUSINESS_BASE = TALENT_SHORTVIDEO_BASE

export const shortvideoRoutes = {
  /** 洞见入口：账号库 / 爆款库 / 进化分析 / 人设融合 */
  insights: `${SHORTVIDEO_BUSINESS_BASE}/insights`,
  /** 与 ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION 同页，router 内 Navigate 重定向 */
  viralAnalysisEvolution: `${SHORTVIDEO_BUSINESS_BASE}/viral-analysis`,
  projects: `${SHORTVIDEO_BUSINESS_BASE}/projects`,
  collect: `${SHORTVIDEO_BUSINESS_BASE}/collect`,
  accounts: `${SHORTVIDEO_BUSINESS_BASE}/accounts`,
  /** 抖音 Web Cookie（Playwright/采集风控时建议配置） */
  douyinCookies: `${SHORTVIDEO_BUSINESS_BASE}/douyin-cookies`,
  benchmarkAccounts: `${SHORTVIDEO_BUSINESS_BASE}/benchmark/accounts`,
  benchmarkVideos: `${SHORTVIDEO_BUSINESS_BASE}/benchmark/videos`,
  benchmarkQualityScripts: `${SHORTVIDEO_BUSINESS_BASE}/benchmark/quality-scripts`,
  benchmarkScriptRecommendation: `${SHORTVIDEO_BUSINESS_BASE}/benchmark/recommendation`,
  remakeTemplates: `${SHORTVIDEO_BUSINESS_BASE}/remake-templates`,
  viralVideos: `${SHORTVIDEO_BUSINESS_BASE}/viral-videos`,
  shotList: `${SHORTVIDEO_BUSINESS_BASE}/shot-list`,
  material: `${SHORTVIDEO_BUSINESS_BASE}/material`,
  editing: `${SHORTVIDEO_BUSINESS_BASE}/editing`,
  publish: `${SHORTVIDEO_BUSINESS_BASE}/publish`,
  quality: `${SHORTVIDEO_BUSINESS_BASE}/quality`,
  daily: `${SHORTVIDEO_BUSINESS_BASE}/daily`,
  viralChain: `${SHORTVIDEO_BUSINESS_BASE}/viral-chain`,
  hotTopics: `${SHORTVIDEO_BUSINESS_BASE}/hot-topics`,
  hotTopicCreate: `${SHORTVIDEO_BUSINESS_BASE}/hot-topics/create`,
  contentCalendar: `${SHORTVIDEO_BUSINESS_BASE}/content-calendar`,
  drama: `${SHORTVIDEO_BUSINESS_BASE}/drama`,
  competitorMonitor: `${SHORTVIDEO_BUSINESS_BASE}/competitor-monitor`,
  effectPredict: `${SHORTVIDEO_BUSINESS_BASE}/effect-predict`,
  dataAnalysis: `${SHORTVIDEO_BUSINESS_BASE}/data-analysis`,
  dashboard: `${SHORTVIDEO_BUSINESS_BASE}/dashboard`,
  quickGenerate: `${SHORTVIDEO_BUSINESS_BASE}/quick-generate`,
  scriptPlanning: `${SHORTVIDEO_BUSINESS_BASE}/script-planning`,
  materialPrepare: `${SHORTVIDEO_BUSINESS_BASE}/material-prepare`,
  materialProduction: `${SHORTVIDEO_BUSINESS_BASE}/material-production`,
  workbench: `${SHORTVIDEO_BUSINESS_BASE}/workbench`,
  workflowEditor: `${SHORTVIDEO_BUSINESS_BASE}/workflow-editor`,
  subtitles: `${SHORTVIDEO_BUSINESS_BASE}/subtitles`,
  personaFusion: `${SHORTVIDEO_BUSINESS_BASE}/persona-fusion`,
  aiMusic: `${SHORTVIDEO_BUSINESS_BASE}/ai-music`,
  seoOptimize: `${SHORTVIDEO_BUSINESS_BASE}/seo-optimize`,
} as const

export type ShortvideoRouteKey = keyof typeof shortvideoRoutes

export const shortvideoLegacyRedirects = {
  seo: {
    from: `${ADMIN_SHORTVIDEO_BASE}/seo`,
    to: shortvideoRoutes.seoOptimize,
    label: 'SEO 优化',
  },
} as const

export const SHORTVIDEO_LEGACY_ROUTE_PATHS: readonly string[] = Object.values(shortvideoLegacyRedirects).map(item => item.from)

export function shortvideoSubtitlePath(id: string | number): string {
  return `${SHORTVIDEO_BUSINESS_BASE}/subtitle-editor/${id}`
}

export function shortvideoBenchmarkVideosPath(accountId?: string | number): string {
  const base = shortvideoRoutes.benchmarkVideos
  return accountId == null || accountId === '' ? base : `${base}?accountId=${accountId}`
}

export function shortvideoBenchmarkAnalysisPath(videoId: string | number): string {
  return `${SHORTVIDEO_BUSINESS_BASE}/benchmark/analysis/${videoId}`
}

export function shortvideoBenchmarkQualityScriptPath(id: string | number): string {
  return `${SHORTVIDEO_BUSINESS_BASE}/benchmark/quality-scripts/${id}`
}

/** 账号详情子页：videos=采集视频列表，analysis=基于该账号全部采集视频的综合分析 */
export function shortvideoAccountDetailPath(
  id: string | number,
  tab?: 'info' | 'videos' | 'analysis'
): string {
  const base = `${SHORTVIDEO_BUSINESS_BASE}/accounts/${id}`
  if (tab === 'videos' || tab === 'analysis') {
    return `${base}?tab=${tab}`
  }
  return base
}

/** 冒烟/回归：只包含无需实体 ID 即可打开的入口页。 */
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
  shortvideoRoutes.benchmarkAccounts,
  shortvideoRoutes.benchmarkVideos,
  shortvideoRoutes.benchmarkQualityScripts,
  shortvideoRoutes.benchmarkScriptRecommendation,
  shortvideoRoutes.remakeTemplates,
  shortvideoRoutes.viralVideos,
  shortvideoRoutes.drama,
  shortvideoRoutes.quality,
  shortvideoRoutes.viralChain,
  shortvideoRoutes.competitorMonitor,
  shortvideoRoutes.effectPredict,
  shortvideoRoutes.dataAnalysis,
  shortvideoRoutes.workflowEditor,
  shortvideoRoutes.subtitles,
  shortvideoRoutes.personaFusion,
  shortvideoRoutes.aiMusic,
  shortvideoRoutes.seoOptimize,
]
