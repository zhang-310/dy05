import { Box, Typography } from '@mui/material'
import NavigateNextIcon from '@mui/icons-material/NavigateNext'
import { Link } from 'react-router-dom'

export const PATH_LABELS: Record<string, string> = {
  admin: '管理后台',
  org: '机构',
  talent: '达人',
  dashboard: '工作台',
  auth: '认证',
  users: '用户管理',
  roles: '角色管理',
  resources: '资源管理',
  'login-logs': '登录日志',
  log: '日志',
  operation: '操作日志',
  system: '系统日志',
  config: '配置管理',
  storage: '存储管理',
  douyin: '抖音',
  accounts: '账号管理',
  personas: '人设管理',
  videos: '视频分析',
  copy: '文案',
  library: '文案库',
  approval: '文案审批',
  template: '文案模板',
  shortvideo: '短视频',
  /** 洞见与拆解、采集、生产等子路径（与 router slug 一致） */
  insights: '洞见中心',
  'viral-videos': '爆款视频库',
  'viral-analysis': '进化爆款分析',
  collect: '账号采集',
  'douyin-cookies': '抖音 Cookie',
  'remake-templates': '改编模板',
  'persona-fusion': '人设融合爆款',
  'viral-chain': '传播链分析',
  'quick-generate': '快速生成',
  projects: '短视频项目',
  workbench: '项目工作台',
  'script-planning': '脚本策划',
  'shot-list': '分镜设计',
  material: '素材管理',
  'material-prepare': '素材准备',
  'material-production': '素材生产',
  editing: '视频生成',
  publish: '发布调度',
  'content-calendar': '内容日历',
  daily: '日排内容',
  'hot-topics': '热点话题',
  create: '新建',
  drama: '剧情短视频',
  quality: '质量看板',
  'competitor-monitor': '竞品监控',
  'effect-predict': '效果预测',
  'data-analysis': '数据分析',
  'workflow-editor': '工作流编辑',
  'subtitle-editor': '字幕编辑',
  'ai-music': 'AI配乐',
  'seo-optimize': 'SEO优化',
  category: '分类管理',
  statistics: '视频统计',
  live: '直播',
  sessions: '直播场次',
  data: '直播数据',
  script: '话术',
  list: '话术管理',
  violation: '违规词',
  product: '商品管理',
  abtest: 'A/B 测试',
  agent: 'AI 智能体',
  ai: 'AI 智能中心',
  knowledge: '知识库',
  'knowledge-source': '知识库管理',
  model: '模型配置',
  evolution: '进化任务',
  topics: '主题池',
  creative: '创意工坊',
  monitoring: '监控中心',
  infra: '基础设施',
  wecom: '企业微信',
  robots: '机器人',
  rules: '推送规则',
}

interface LayoutBreadcrumbsProps {
  pathname: string
}

/** 同一路径段在不同父路径下语义不同（如 accounts、dashboard） */
function breadcrumbSegmentLabel(parts: string[], index: number): string {
  const p = parts[index]
  const parent = index > 0 ? parts[index - 1] : ''
  if (parent === 'shortvideo' && p === 'accounts') return '短视频账号'
  if (parent === 'shortvideo' && p === 'dashboard') return '短视频首页'
  return PATH_LABELS[p] || p
}

export function LayoutBreadcrumbs({ pathname }: LayoutBreadcrumbsProps) {
  const parts = pathname.split('/').filter(Boolean)
  if (parts.length === 0) return null
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: 'text.secondary', fontSize: 14 }}>
      <Link to="/" style={{ color: 'inherit', textDecoration: 'none' }}>
        首页
      </Link>
      {parts.map((_, i) => {
        const path = '/' + parts.slice(0, i + 1).join('/')
        const label = breadcrumbSegmentLabel(parts, i)
        const isLast = i === parts.length - 1
        return (
          <Box key={path} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <NavigateNextIcon sx={{ fontSize: 18, color: 'text.disabled' }} />
            {isLast ? (
              <Typography component="span" sx={{ color: 'text.primary', fontWeight: 500 }}>
                {label}
              </Typography>
            ) : (
              <Link to={path} style={{ color: 'inherit', textDecoration: 'none' }}>
                {label}
              </Link>
            )}
          </Box>
        )
      })}
    </Box>
  )
}
