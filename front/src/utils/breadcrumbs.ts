const ROUTE_LABELS: Record<string, string> = {
  '/admin/dashboard': '首页',
  '/admin/auth/users': '用户管理',
  '/admin/auth/roles': '角色管理',
  '/admin/auth/login-logs': '登录日志',
  '/admin/douyin/accounts': '抖音账号',
  '/admin/douyin/personas': '人设管理',
  '/admin/live/sessions': '直播场次',
  '/admin/live/scripts': '直播话术',
  '/admin/product/list': '商品管理',
  '/admin/script/list': '话术脚本',
  '/admin/script/violation-words': '违规词库',
  '/admin/shortvideo/projects': '短视频项目',
  '/admin/shortvideo/hot-topics/create': '热点借势创作',
  '/admin/shortvideo/douyin-cookies': '抖音 Cookie 管理',
  '/admin/copy/library': '文案库',
  '/admin/copy/templates': '文案模板',
  '/admin/ai/knowledge': '知识库',
  '/admin/ai/evolution': '自进化引擎',
  '/admin/ai/agent/list': '智能体列表',
  '/admin/ai/agent/market': '智能体市场',
  '/admin/ai/agent/chat': '智能体对话',
  '/admin/ai/abtest/experiments': 'A/B 实验',
  '/admin/config': '系统配置',
  '/admin/storage': '文件存储',
  '/admin/system': '系统监控',
  '/admin/log/operations': '操作日志',
  '/admin/wecom/robots': '企业微信机器人',
  '/admin/payment/orders': '支付订单',
  '/admin/attribution': '归因分析',
}

const GROUP_LABELS: Record<string, string> = {
  '/admin/auth': '系统管理',
  '/admin/douyin': '运营管理',
  '/admin/live': '运营管理',
  '/admin/product': '运营管理',
  '/admin/script': '运营管理',
  '/admin/shortvideo': '运营管理',
  '/admin/copy': '运营管理',
  '/admin/ai': 'AI 中心',
  '/admin/ai/agent': 'AI 中心',
  '/admin/ai/abtest': 'AI 中心',
  '/admin/wecom': 'AI 中心',
  '/admin/config': '系统管理',
  '/admin/storage': '系统管理',
  '/admin/system': '系统管理',
  '/admin/log': '系统管理',
  '/admin/attribution': '系统管理',
  '/admin/payment': '支付',
}

export interface Crumb { label: string; path?: string }

export function getBreadcrumbs(pathname: string): Crumb[] {
  if (pathname === '/admin/dashboard') return [{ label: '首页' }]
  const group = Object.keys(GROUP_LABELS).find(k => pathname.startsWith(k))
  const label = ROUTE_LABELS[pathname]
  const crumbs: Crumb[] = [{ label: '首页', path: '/admin/dashboard' }]
  if (group) crumbs.push({ label: GROUP_LABELS[group] })
  if (label) crumbs.push({ label })
  return crumbs
}
