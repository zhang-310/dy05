import { useState, useEffect, useMemo } from 'react'
import {
  Box,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Divider,
} from '@mui/material'
import DashboardIcon from '@mui/icons-material/Dashboard'
import PeopleIcon from '@mui/icons-material/People'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import LiveTvIcon from '@mui/icons-material/LiveTv'
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag'
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
import { useNavigate, useLocation } from 'react-router-dom'
import { BaseLayout } from './BaseLayout'
import {
  shortvideoRoutes,
  shortvideoSubtitlePath,
  ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION,
} from '@/constants/shortvideoRoutes'

const RAIL_WIDTH = 72
const SUB_WIDTH = 156

interface NavChild {
  label: string
  icon: React.ReactNode
  path: string
  section?: string  // 分组标题，渲染时在该项上方显示
}

interface NavGroup {
  key: string
  label: string
  icon: React.ReactNode
  path?: string
  children?: NavChild[]
}

const NAV_GROUPS: NavGroup[] = [
  {
    key: 'dashboard',
    label: '首页',
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
    key: 'douyin',
    label: '抖音',
    icon: <BusinessIcon />,
    children: [
      { label: '抖音账号', icon: <BusinessIcon fontSize="small" />, path: '/admin/douyin/accounts' },
      { label: '视频列表', icon: <VideoLibraryIcon fontSize="small" />, path: '/admin/douyin/videos' },
    ],
  },
  {
    key: 'shortvideo',
    label: '短视频',
    icon: <VideoLibraryIcon />,
    children: [
      // 快速入口
      { label: '短视频首页', icon: <MonitorIcon fontSize="small" />, path: shortvideoRoutes.dashboard, section: '快速入口' },
      { label: '快速生成', icon: <SmartToyIcon fontSize="small" />, path: shortvideoRoutes.quickGenerate },
      // 项目管理
      { label: '短视频项目', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.projects, section: '项目管理' },
      { label: '项目工作台', icon: <AssignmentIcon fontSize="small" />, path: shortvideoRoutes.workbench },
      // 创作
      { label: '脚本策划', icon: <ArticleIcon fontSize="small" />, path: shortvideoRoutes.scriptPlanning, section: '创作' },
      { label: '分镜设计', icon: <ArticleIcon fontSize="small" />, path: shortvideoRoutes.shotList },
      { label: '素材管理', icon: <StorageIcon fontSize="small" />, path: shortvideoRoutes.material },
      { label: '素材准备', icon: <StorageIcon fontSize="small" />, path: shortvideoRoutes.materialPrepare },
      { label: '素材生产', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.materialProduction },
      { label: '视频生成', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.editing },
      // 内容运营
      { label: '发布调度', icon: <AssignmentIcon fontSize="small" />, path: shortvideoRoutes.publish, section: '内容运营' },
      { label: '内容日历', icon: <AssignmentIcon fontSize="small" />, path: shortvideoRoutes.contentCalendar },
      { label: '日排内容', icon: <AssignmentIcon fontSize="small" />, path: shortvideoRoutes.daily },
      { label: '热点话题', icon: <ArticleIcon fontSize="small" />, path: shortvideoRoutes.hotTopics },
      { label: '热点借势创作', icon: <PsychologyIcon fontSize="small" />, path: shortvideoRoutes.hotTopicCreate },
      // 洞见与拆解（采集 → 资产 → 拆解 → 应用）
      { label: '洞见中心', icon: <TrackChangesIcon fontSize="small" />, path: shortvideoRoutes.insights, section: '洞见与拆解' },
      { label: '短视频账号', icon: <BusinessIcon fontSize="small" />, path: shortvideoRoutes.accounts },
      { label: '爆款视频库', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.viralVideos },
      { label: '进化爆款分析', icon: <SmartToyIcon fontSize="small" />, path: shortvideoRoutes.viralAnalysisEvolution },
      { label: '人设融合爆款', icon: <PsychologyIcon fontSize="small" />, path: shortvideoRoutes.personaFusion },
      // 模板 & 采集
      { label: '账号采集', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.collect, section: '模板 & 采集' },
      { label: '抖音 Cookie', icon: <VpnKeyIcon fontSize="small" />, path: shortvideoRoutes.douyinCookies },
      { label: '改编模板', icon: <ArticleIcon fontSize="small" />, path: shortvideoRoutes.remakeTemplates },
      { label: '剧情短视频', icon: <VideoLibraryIcon fontSize="small" />, path: shortvideoRoutes.drama },
      // 分析
      { label: '质量看板', icon: <MonitorIcon fontSize="small" />, path: shortvideoRoutes.quality, section: '分析' },
      { label: '传播链分析', icon: <TrackChangesIcon fontSize="small" />, path: shortvideoRoutes.viralChain },
      { label: '竞品监控', icon: <TrackChangesIcon fontSize="small" />, path: shortvideoRoutes.competitorMonitor },
      { label: '效果预测', icon: <ScienceIcon fontSize="small" />, path: shortvideoRoutes.effectPredict },
      { label: '数据分析', icon: <MonitorIcon fontSize="small" />, path: shortvideoRoutes.dataAnalysis },
      // 工具
      { label: '工作流编辑', icon: <SettingsIcon fontSize="small" />, path: shortvideoRoutes.workflowEditor, section: '工具' },
      { label: '字幕编辑', icon: <ArticleIcon fontSize="small" />, path: shortvideoSubtitlePath(0) },
      { label: 'AI配乐', icon: <SmartToyIcon fontSize="small" />, path: shortvideoRoutes.aiMusic },
      { label: 'SEO优化', icon: <ScienceIcon fontSize="small" />, path: shortvideoRoutes.seoOptimize },
    ],
  },
  {
    key: 'product',
    label: '商品',
    icon: <ShoppingBagIcon />,
    children: [
      { label: '商品管理', icon: <ShoppingBagIcon fontSize="small" />, path: '/admin/product/list' },
      { label: '销售记录', icon: <ShoppingBagIcon fontSize="small" />, path: '/admin/product/sales-history' },
      { label: '话术效果', icon: <TrackChangesIcon fontSize="small" />, path: '/admin/product/effectiveness' },
      { label: '风格预设', icon: <SettingsIcon fontSize="small" />, path: '/admin/product/style-presets' },
      { label: '上播准备度', icon: <AssignmentIcon fontSize="small" />, path: '/admin/product/readiness' },
    ],
  },
  {
    key: 'content',
    label: '内容',
    icon: <ArticleIcon />,
    children: [
      { label: '话术脚本', icon: <ArticleIcon fontSize="small" />, path: '/admin/script/list' },
      { label: '违规词库', icon: <ArticleIcon fontSize="small" />, path: '/admin/script/violation-words' },
      { label: '违规检测', icon: <AssignmentIcon fontSize="small" />, path: '/admin/script/violation-check' },
      { label: '话术模板', icon: <ArticleIcon fontSize="small" />, path: '/admin/script/templates' },
      { label: '混合搜索', icon: <PsychologyIcon fontSize="small" />, path: '/admin/script/hybrid-search' },
      { label: '脚本生成', icon: <SmartToyIcon fontSize="small" />, path: '/admin/script/generation' },
      { label: '脚本优化', icon: <SmartToyIcon fontSize="small" />, path: '/admin/script/optimization' },
      { label: '文案库', icon: <ArticleIcon fontSize="small" />, path: '/admin/copy/library' },
      { label: '文案模板', icon: <ArticleIcon fontSize="small" />, path: '/admin/copy/templates' },
      { label: '文案审批', icon: <AssignmentIcon fontSize="small" />, path: '/admin/copy/approval' },
      { label: '内容库', icon: <ArticleIcon fontSize="small" />, path: '/admin/content/library' },
    ],
  },
  {
    key: 'live',
    label: '直播',
    icon: <LiveTvIcon />,
    children: [
      { label: '直播场次', icon: <LiveTvIcon fontSize="small" />, path: '/admin/live/sessions' },
      { label: '话术脚本', icon: <ArticleIcon fontSize="small" />, path: '/admin/live/scripts' },
      { label: '商品管理', icon: <ShoppingBagIcon fontSize="small" />, path: '/admin/live/products' },
      { label: '新建场次', icon: <LiveTvIcon fontSize="small" />, path: '/admin/live/sessions/create' },
      { label: '话术排行', icon: <TrackChangesIcon fontSize="small" />, path: '/admin/live/ranking' },
      { label: '节奏配置', icon: <SettingsIcon fontSize="small" />, path: '/admin/live/rhythm' },
      { label: '版本对比', icon: <ArticleIcon fontSize="small" />, path: '/admin/live/history-compare' },
    ],
  },
  {
    key: 'ai',
    label: 'AI中心',
    icon: <SmartToyIcon />,
    children: [
      // 核心
      { label: 'AI仪表盘', icon: <MonitorIcon fontSize="small" />, path: '/admin/ai/dashboard', section: '核心' },
      { label: '知识库', icon: <PsychologyIcon fontSize="small" />, path: '/admin/ai/knowledge' },
      { label: '行业大脑', icon: <PsychologyIcon fontSize="small" />, path: '/admin/ai/industry-brain' },
      { label: '大脑诊断', icon: <LocalHospitalIcon fontSize="small" />, path: '/admin/ai/brain-diagnosis' },
      // 进化引擎
      { label: '自进化引擎', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/evolution', section: '进化引擎' },
      { label: '进化监控看板', icon: <PsychologyIcon fontSize="small" />, path: '/admin/ai/knowledge-evolution' },
      // Prompt & 模型
      { label: 'Prompt工作台', icon: <ScienceIcon fontSize="small" />, path: '/admin/ai/prompt-tools', section: 'Prompt & 模型' },
      { label: '模型配置', icon: <SettingsIcon fontSize="small" />, path: '/admin/ai/models-config' },
      { label: '任务模型映射', icon: <SettingsIcon fontSize="small" />, path: '/admin/ai/task-model-config' },
      { label: '模型基准测试', icon: <ScienceIcon fontSize="small" />, path: '/admin/ai/model-benchmark' },
      // AI应用
      { label: '进化爆款分析', icon: <TrackChangesIcon fontSize="small" />, path: ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION, section: 'AI应用' },
      { label: '创意工坊', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/creative-studio' },
      { label: '数字人', icon: <PeopleIcon fontSize="small" />, path: '/admin/ai/digital-human' },
      { label: '智能体列表', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/agent/list' },
      { label: '智能体市场', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/agent/market' },
      { label: '工作流编排', icon: <SmartToyIcon fontSize="small" />, path: '/admin/ai/agent/workflow/list' },
      { label: 'A/B实验', icon: <ScienceIcon fontSize="small" />, path: '/admin/ai/abtest/experiments' },
      { label: '归因分析', icon: <TrackChangesIcon fontSize="small" />, path: '/admin/attribution' },
      // 运维
      { label: 'AI监控', icon: <MonitorIcon fontSize="small" />, path: '/admin/ai/monitoring', section: '运维' },
      { label: 'AI配额管理', icon: <MonitorIcon fontSize="small" />, path: '/admin/ai/quota' },
    ],
  },
  {
    key: 'system',
    label: '系统',
    icon: <SettingsIcon />,
    children: [
      // 权限
      { label: '用户管理', icon: <PeopleIcon fontSize="small" />, path: '/admin/auth/users', section: '权限' },
      { label: '角色管理', icon: <VpnKeyIcon fontSize="small" />, path: '/admin/auth/roles' },
      { label: '资源权限', icon: <VpnKeyIcon fontSize="small" />, path: '/admin/auth/resources' },
      // 配置
      { label: '系统配置', icon: <SettingsIcon fontSize="small" />, path: '/admin/config', section: '配置' },
      { label: '文件存储', icon: <StorageIcon fontSize="small" />, path: '/admin/storage' },
      // 监控
      { label: '系统监控', icon: <MonitorIcon fontSize="small" />, path: '/admin/system', section: '监控' },
      { label: '告警规则', icon: <MonitorIcon fontSize="small" />, path: '/admin/system/alert-rules' },
      { label: '合规检测', icon: <AssignmentIcon fontSize="small" />, path: '/admin/system/compliance' },
      { label: '性能监控', icon: <MonitorIcon fontSize="small" />, path: '/admin/system/performance' },
      { label: 'API健康监控', icon: <MonitorIcon fontSize="small" />, path: '/admin/system/external-api-health' },
      { label: '天API面板', icon: <LogoDevIcon fontSize="small" />, path: '/admin/system/tianapi' },
      // 日志
      { label: 'API日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/system/api-log', section: '日志' },
      { label: '操作日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/log/operations' },
      { label: '审计日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/log/audit' },
      { label: '系统日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/log/system' },
      { label: '同步日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/system/sync-log' },
      { label: '登录日志', icon: <LogoDevIcon fontSize="small" />, path: '/admin/auth/login-logs' },
      // 其他
      { label: '外部API配置', icon: <LogoDevIcon fontSize="small" />, path: '/admin/system/external-api', section: '其他' },
      { label: '俚语词典', icon: <ArticleIcon fontSize="small" />, path: '/admin/slangdict' },
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
]
function AdminNav() {
  const location = useLocation()
  const navigate = useNavigate()

  const activeGroup = useMemo(() => {
    for (const g of NAV_GROUPS) {
      if (g.path && location.pathname === g.path) return g.key
      if (g.children?.some(c => location.pathname === c.path)) return g.key
    }
    return null
  }, [location.pathname])

  const [selectedKey, setSelectedKey] = useState<string | null>(activeGroup)

  useEffect(() => {
    setSelectedKey(activeGroup)
  }, [activeGroup])

  const selectedGroup = NAV_GROUPS.find(g => g.key === selectedKey && g.children)

  const handleRailClick = (group: NavGroup) => {
    if (group.path) {
      navigate(group.path)
      setSelectedKey(group.key)
    } else {
      setSelectedKey(prev => (prev === group.key ? null : group.key))
    }
  }

  return (
    <Box sx={{ display: 'flex', height: '100%' }}>
      {/* === Rail (第一列) === */}
      <Box
        sx={{
          width: RAIL_WIDTH,
          flexShrink: 0,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          bgcolor: 'white',
          borderRight: '1px solid',
          borderColor: 'divider',
          height: '100%',
          overflowY: 'auto',
        }}
      >
        {NAV_GROUPS.map((group) => {
          const isActive =
            group.key === selectedKey ||
            (group.path ? location.pathname === group.path : false)
          return (
            <Box
              key={group.key}
              onClick={() => handleRailClick(group)}
              sx={{
                width: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                py: 1.5,
                cursor: 'pointer',
                color: isActive ? 'primary.main' : 'text.secondary',
                bgcolor: isActive ? 'primary.50' : 'transparent',
                borderRight: isActive ? '2px solid' : '2px solid transparent',
                borderColor: isActive ? 'primary.main' : 'transparent',
                '&:hover': { bgcolor: 'grey.100', color: 'primary.main' },
                transition: 'all 0.15s',
                gap: 0.5,
              }}
            >
              <Box sx={{ fontSize: 20, display: 'flex', alignItems: 'center' }}>{group.icon}</Box>
              <Typography sx={{ fontSize: 12, lineHeight: 1.2, textAlign: 'center', userSelect: 'none', fontWeight: isActive ? 600 : 400 }}>
                {group.label}
              </Typography>
            </Box>
          )
        })}
      </Box>

      {/* === Sub-menu (第二列) === */}
      {selectedGroup && (
        <Box
          sx={{
            width: SUB_WIDTH,
            flexShrink: 0,
            bgcolor: 'grey.50',
            borderRight: '1px solid',
            borderColor: 'divider',
            height: '100%',
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          {/* Sub-menu header */}
          <Box sx={{ px: 2, py: 1.5 }}>
            <Typography sx={{ fontSize: 12, color: 'text.disabled', fontWeight: 600, letterSpacing: 0.5 }}>
              {selectedGroup.label}
            </Typography>
          </Box>
          <Divider />
          <List disablePadding dense sx={{ flex: 1 }}>
            {selectedGroup.children!.map((child) => {
              const active = location.pathname === child.path
              return (
                <Box key={child.path}>
                  {child.section && (
                    <Box sx={{ px: 1.5, pt: 1.5, pb: 0.5 }}>
                      <Typography sx={{ fontSize: 10, color: 'text.disabled', fontWeight: 700, letterSpacing: 1, textTransform: 'uppercase' }}>
                        {child.section}
                      </Typography>
                    </Box>
                  )}
                <ListItemButton
                  selected={active}
                  onClick={() => navigate(child.path)}
                  sx={{
                    py: 0.8,
                    pl: 1.5,
                    pr: 1,
                    borderRadius: 0,
                    '&.Mui-selected': {
                      bgcolor: 'primary.50',
                      borderRight: '2px solid',
                      borderColor: 'primary.main',
                    },
                    '&.Mui-selected:hover': { bgcolor: 'primary.50' },
                  }}
                >
                  <ListItemIcon sx={{ minWidth: 28, color: active ? 'primary.main' : 'text.disabled' }}>
                    {child.icon}
                  </ListItemIcon>
                  <ListItemText
                    primary={child.label}
                    primaryTypographyProps={{
                      fontSize: 12,
                      color: active ? 'primary.main' : 'text.secondary',
                      fontWeight: active ? 600 : 400,
                    }}
                  />
                </ListItemButton>
                </Box>
              )
            })}
          </List>
        </Box>
      )}
    </Box>
  )
}

export function AdminLayout() {
  return <BaseLayout drawerContent={<AdminNav />} />
}

