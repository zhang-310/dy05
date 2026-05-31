import type React from 'react'
import DashboardIcon from '@mui/icons-material/Dashboard'
import PeopleIcon from '@mui/icons-material/People'
import ArticleIcon from '@mui/icons-material/Article'
import AssignmentIcon from '@mui/icons-material/Assignment'
import SettingsIcon from '@mui/icons-material/Settings'
import StorageIcon from '@mui/icons-material/Storage'
import MonitorIcon from '@mui/icons-material/Monitor'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import ScienceIcon from '@mui/icons-material/Science'
import PsychologyIcon from '@mui/icons-material/Psychology'
import LocalHospitalIcon from '@mui/icons-material/LocalHospital'
import BusinessIcon from '@mui/icons-material/Business'
import PaymentIcon from '@mui/icons-material/Payment'
import TrackChangesIcon from '@mui/icons-material/TrackChanges'
import LogoDevIcon from '@mui/icons-material/LogoDev'
import VpnKeyIcon from '@mui/icons-material/VpnKey'
import LiveTvIcon from '@mui/icons-material/LiveTv'
import BarChartIcon from '@mui/icons-material/BarChart'
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import { ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION, shortvideoRoutes } from '@/constants/shortvideoRoutes'

export interface NavChild {
  label: string
  icon: React.ReactNode
  path: string
  section?: string
}

export interface NavGroup {
  key: string
  label: string
  icon: React.ReactNode
  path?: string
  children?: NavChild[]
}

export const ADMIN_NAV_GROUPS: NavGroup[] = [
  {
    key: 'dashboard',
    label: '总控',
    icon: <DashboardIcon />,
    path: '/admin/dashboard',
  },
  {
    key: 'kpi',
    label: '统一KPI',
    icon: <TrackChangesIcon />,
    path: '/admin/kpi',
  },
  {
    key: 'ai',
    label: 'AI中心',
    icon: <SmartToyIcon />,
    children: [
      { label: 'AI仪表盘', icon: <MonitorIcon fontSize="small" />, path: '/admin/ai/dashboard', section: '核心' },
      { label: '知识库', icon: <PsychologyIcon fontSize="small" />, path: '/admin/ai/knowledge' },
      { label: '行业大脑', icon: <PsychologyIcon fontSize="small" />, path: '/admin/ai/industry-brain' },
      { label: '大脑诊断', icon: <LocalHospitalIcon fontSize="small" />, path: '/admin/ai/brain-diagnosis' },
      { label: '自进化引擎', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/evolution', section: '进化引擎' },
      { label: '进化监控看板', icon: <PsychologyIcon fontSize="small" />, path: '/admin/ai/knowledge-evolution' },
      { label: 'Prompt工作台', icon: <ScienceIcon fontSize="small" />, path: '/admin/ai/prompt-tools', section: 'Prompt & 模型' },
      { label: '模型配置', icon: <SettingsIcon fontSize="small" />, path: '/admin/ai/models-config' },
      { label: '任务模型映射', icon: <SettingsIcon fontSize="small" />, path: '/admin/ai/task-model-config' },
      { label: '模型基准测试', icon: <ScienceIcon fontSize="small" />, path: '/admin/ai/model-benchmark' },
      { label: '进化爆款分析', icon: <TrackChangesIcon fontSize="small" />, path: ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION, section: 'AI应用' },
      { label: '创意工坊', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/creative-studio' },
      { label: '数字人', icon: <PeopleIcon fontSize="small" />, path: '/admin/ai/digital-human' },
      { label: '智能体列表', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/agent/list' },
      { label: '智能体市场', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/agent/market' },
      { label: '工作流编排', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/agent/workflow/list' },
      { label: 'A/B实验', icon: <ScienceIcon fontSize="small" />, path: '/admin/ai/abtest/experiments' },
      { label: '归因分析', icon: <TrackChangesIcon fontSize="small" />, path: '/admin/attribution' },
      { label: 'AI监控', icon: <MonitorIcon fontSize="small" />, path: '/admin/ai/monitoring', section: '运维' },
      { label: '采集进化状态', icon: <MonitorIcon fontSize="small" />, path: '/admin/ai/runtime-status' },
      { label: 'AI配额管理', icon: <MonitorIcon fontSize="small" />, path: '/admin/ai/quota' },
    ],
  },
  {
    key: 'system',
    label: '系统',
    icon: <SettingsIcon />,
    children: [
      { label: '用户管理', icon: <PeopleIcon fontSize="small" />, path: '/admin/auth/users', section: '权限' },
      { label: '角色管理', icon: <VpnKeyIcon fontSize="small" />, path: '/admin/auth/roles' },
      { label: '资源权限', icon: <VpnKeyIcon fontSize="small" />, path: '/admin/auth/resources' },
      { label: '系统配置', icon: <SettingsIcon fontSize="small" />, path: '/admin/config', section: '配置' },
      { label: '文件存储', icon: <StorageIcon fontSize="small" />, path: '/admin/storage' },
      { label: '系统监控', icon: <MonitorIcon fontSize="small" />, path: '/admin/system', section: '监控' },
      { label: '平台监控', icon: <MonitorIcon fontSize="small" />, path: '/admin/monitoring' },
      { label: '告警规则', icon: <MonitorIcon fontSize="small" />, path: '/admin/system/alert-rules' },
      { label: '合规检测', icon: <AssignmentIcon fontSize="small" />, path: '/admin/system/compliance' },
      { label: '性能监控', icon: <MonitorIcon fontSize="small" />, path: '/admin/system/performance' },
      { label: 'API健康监控', icon: <MonitorIcon fontSize="small" />, path: '/admin/system/external-api-health' },
      { label: '天API面板', icon: <LogoDevIcon fontSize="small" />, path: '/admin/system/tianapi' },
      { label: 'API日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/system/api-log', section: '日志' },
      { label: '操作日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/log/operations' },
      { label: '审计日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/log/audit' },
      { label: '系统日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/log/system' },
      { label: '同步日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/system/sync-log' },
      { label: '登录日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/auth/login-logs' },
      { label: '外部API配置', icon: <LogoDevIcon fontSize="small" />, path: '/admin/system/external-api', section: '其他' },
      { label: '企业微信推送', icon: <BusinessIcon fontSize="small" />, path: '/admin/wecom' },
    ],
  },
  {
    key: 'payment',
    label: '支付',
    icon: <PaymentIcon />,
    children: [
      { label: '订单管理', icon: <PaymentIcon fontSize="small" />, path: '/admin/payment/orders' },
      { label: '订阅管理', icon: <PaymentIcon fontSize="small" />, path: '/admin/payment/subscription' },
      { label: '额度使用', icon: <TrackChangesIcon fontSize="small" />, path: '/admin/payment/usage' },
    ],
  },
  {
    key: 'gaifan',
    label: '商业化',
    icon: <LogoDevIcon />,
    children: [
      { label: '积分治理', icon: <TrackChangesIcon fontSize="small" />, path: '/admin/gaifan/credits' },
      { label: 'MCP 网关', icon: <SmartToyIcon fontSize="small" />, path: '/admin/gaifan/mcp' },
      { label: 'OpenAPI', icon: <LogoDevIcon fontSize="small" />, path: '/admin/gaifan/openapi' },
      { label: '支付中心', icon: <PaymentIcon fontSize="small" />, path: '/admin/gaifan/payment' },
      { label: '运营指挥官', icon: <SmartToyIcon fontSize="small" />, path: '/admin/gaifan/douyin-ops-commander' },
      { label: '官网产品', icon: <LogoDevIcon fontSize="small" />, path: '/official' },
    ],
  },
  {
    key: 'profile',
    label: '个人',
    icon: <AccountCircleIcon />,
    path: '/admin/profile',
  },
]

export const ORG_NAV_ITEMS: NavChild[] = [
  { label: '工作台', icon: <DashboardIcon fontSize="small" />, path: '/org/dashboard' },
  { label: '成员管理', icon: <PeopleIcon fontSize="small" />, path: '/org/members' },
  { label: '数据分析', icon: <BarChartIcon fontSize="small" />, path: '/org/analytics' },
  { label: '直播场次', icon: <LiveTvIcon fontSize="small" />, path: '/org/live/sessions' },
  { label: '直播话术', icon: <ArticleIcon fontSize="small" />, path: '/org/live/scripts' },
  { label: '直播商品', icon: <ShoppingBagIcon fontSize="small" />, path: '/org/live/products' },
  { label: '节奏配置', icon: <TrackChangesIcon fontSize="small" />, path: '/org/live/rhythm' },
  { label: '复盘审核', icon: <AssignmentIcon fontSize="small" />, path: '/org/live/reviews' },
  { label: '商品管理', icon: <ShoppingBagIcon fontSize="small" />, path: '/org/product/list' },
  { label: '上播准备度', icon: <AssignmentIcon fontSize="small" />, path: '/org/product/readiness' },
  { label: '话术脚本', icon: <ArticleIcon fontSize="small" />, path: '/org/script/list' },
  { label: '违规检测', icon: <AssignmentIcon fontSize="small" />, path: '/org/script/violation-check' },
  { label: '内容库', icon: <ArticleIcon fontSize="small" />, path: '/org/content/library' },
  { label: '俚语词典', icon: <ArticleIcon fontSize="small" />, path: '/org/slangdict' },
  { label: '个人中心', icon: <AccountCircleIcon fontSize="small" />, path: '/org/profile' },
]

export const TALENT_NAV_ITEMS: NavChild[] = [
  { label: '工作台', icon: <DashboardIcon fontSize="small" />, path: '/talent/dashboard' },
  { label: '直播场次', icon: <LiveTvIcon fontSize="small" />, path: '/talent/live/sessions' },
  { label: '短视频首页', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.dashboard },
  { label: '项目列表', icon: <VideoLibraryIcon fontSize="small" />, path: '/talent/shortvideo' },
  { label: '快速生成', icon: <SmartToyIcon fontSize="small" />, path: shortvideoRoutes.quickGenerate },
  { label: '脚本策划', icon: <ArticleIcon fontSize="small" />, path: shortvideoRoutes.scriptPlanning },
  { label: '分镜设计', icon: <ArticleIcon fontSize="small" />, path: shortvideoRoutes.shotList },
  { label: '素材生产', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.materialProduction },
  { label: '视频生成', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.editing },
  { label: '发布调度', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.publish },
  { label: '洞见中心', icon: <TrackChangesIcon fontSize="small" />, path: shortvideoRoutes.insights },
  { label: '爆款视频库', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.viralVideos },
  { label: '账号采集', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.collect },
  { label: '对标账号', icon: <BusinessIcon fontSize="small" />, path: shortvideoRoutes.benchmarkAccounts },
  { label: '对标视频', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.benchmarkVideos },
  { label: '抖音 Cookie', icon: <VpnKeyIcon fontSize="small" />, path: shortvideoRoutes.douyinCookies },
  { label: '抖音账号', icon: <BusinessIcon fontSize="small" />, path: '/talent/douyin/accounts' },
  { label: '个人中心', icon: <AccountCircleIcon fontSize="small" />, path: '/talent/profile' },
]

export const USER_NAV_ITEMS: NavChild[] = [
  { label: '个人工作台', icon: <DashboardIcon fontSize="small" />, path: '/user/dashboard' },
  { label: '我的短视频', icon: <VideoLibraryIcon fontSize="small" />, path: '/user/shortvideo' },
  { label: 'AI 创作', icon: <SmartToyIcon fontSize="small" />, path: '/user/shortvideo/create' },
  { label: '脚本策划', icon: <ArticleIcon fontSize="small" />, path: '/user/shortvideo/planning' },
  { label: '素材准备', icon: <VideoLibraryIcon fontSize="small" />, path: '/user/shortvideo/materials' },
  { label: '发布检查', icon: <AssignmentIcon fontSize="small" />, path: '/user/shortvideo/publish' },
  { label: '个人中心', icon: <AccountCircleIcon fontSize="small" />, path: '/user/profile' },
]
