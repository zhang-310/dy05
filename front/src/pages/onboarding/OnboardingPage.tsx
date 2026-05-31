import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert, Box, Button, Card, CardContent, Chip, Grid, Paper, Step, StepContent,
  StepLabel, Stepper, Typography,
} from '@mui/material'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import InventoryIcon from '@mui/icons-material/Inventory'
import LiveTvIcon from '@mui/icons-material/LiveTv'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { aiApi } from '@/api/ai'
import { dashboardApi } from '@/api/dashboard'
import { productApi } from '@/api/product'
import { storageApi } from '@/api/storage'
import { getErrorMessage } from '@/utils/errorHandler'

const STEPS = [
  {
    label: '绑定抖音账号',
    icon: <AccountCircleIcon />,
    description: '进入抖音账号管理，绑定账号并检查 OAuth Token 状态。账号同步后，直播、短视频和爆款分析才有真实数据来源。',
    action: '去绑定账号',
    path: '/talent/douyin/accounts',
  },
  {
    label: '导入商品',
    icon: <InventoryIcon />,
    description: '维护商品、价格、库存和卖点。商品信息会被商品话术、销售记录、短视频项目和直播脚本共用。',
    action: '去添加商品',
    path: '/org/product/list',
  },
  {
    label: '创建第一场直播',
    icon: <LiveTvIcon />,
    description: '创建直播场次并选择商品。直播脚本、实时面板和复盘都依赖已入库的场次和商品关系。',
    action: '去创建场次',
    path: '/org/live/sessions/create',
  },
  {
    label: '创建短视频项目',
    icon: <VideoLibraryIcon />,
    description: '短视频项目负责承接脚本、分镜、素材、成片和发布任务。需要先有商品或人设数据，才能减少空项目。',
    action: '去短视频项目',
    path: '/talent/shortvideo/projects',
  },
  {
    label: '配置 AI 模型',
    icon: <SmartToyIcon />,
    description: '检查模型配置、Ollama/第三方模型连通性和额度。AI 话术、向量检索、爆款分析都依赖可用模型。',
    action: '去模型配置',
    path: '/admin/ai/models-config',
  },
]

const ONBOARDING_READY_ENDPOINTS = [
  '/product/search',
  '/ai/admin/models/list',
  '/storage/configured',
  '/dashboard/kpi-unified',
].join('|')

const ONBOARDING_NAVIGATION_TARGETS = STEPS.map(step => step.path).join('|')

const ONBOARDING_UNSUPPORTED_ACTIONS = [
  'local-readiness-complete',
  'local-product-fallback',
  'local-model-fallback',
  'local-storage-fallback',
  'local-kpi-fallback',
  'auto-create-account',
  'auto-create-product',
  'auto-create-live-session',
  'auto-create-shortvideo-project',
  'auto-create-ai-model',
].join('|')

export default function OnboardingPage() {
  const navigate = useNavigate()
  const [activeStep, setActiveStep] = useState(0)
  const productsQuery = useQuery({ queryKey: ['onboarding-products'], queryFn: () => productApi.list({ page: 0, rows: 1 }) })
  const modelsQuery = useQuery({ queryKey: ['onboarding-ai-models'], queryFn: () => aiApi.adminModelsList() })
  const storageQuery = useQuery({ queryKey: ['onboarding-storage-configured'], queryFn: () => storageApi.configured() })
  const kpiQuery = useQuery({ queryKey: ['onboarding-kpi'], queryFn: () => dashboardApi.kpiUnified(7) })

  const handleNext = () => setActiveStep((s) => Math.min(s + 1, STEPS.length))
  const handleBack = () => setActiveStep((s) => Math.max(s - 1, 0))
  const handleGo = (path: string) => navigate(path)

  const healthCards = [
    {
      title: '商品数据源',
      source: '/product/search',
      status: productsQuery.isPending ? 'checking' : productsQuery.isError ? 'error' : 'ok',
      description: productsQuery.isPending
        ? '正在检查商品检索接口...'
        : productsQuery.isError
          ? `检查失败：/product/search - ${getErrorMessage(productsQuery.error)}`
          : `已接入 /product/search，当前可见 ${productsQuery.data?.total ?? 0} 个商品。`,
    },
    {
      title: 'AI 模型源',
      source: '/ai/admin/models/list',
      status: modelsQuery.isPending ? 'checking' : modelsQuery.isError ? 'error' : 'ok',
      description: modelsQuery.isPending
        ? '正在检查模型配置接口...'
        : modelsQuery.isError
          ? `检查失败：/ai/admin/models/list - ${getErrorMessage(modelsQuery.error)}`
          : `已接入 /ai/admin/models/list，当前 ${modelsQuery.data?.length ?? 0} 个模型配置。`,
    },
    {
      title: '对象存储',
      source: '/storage/configured',
      status: storageQuery.isPending ? 'checking' : storageQuery.isError ? 'error' : storageQuery.data ? 'ok' : 'warn',
      description: storageQuery.isPending
        ? '正在检查 BOS 存储配置...'
        : storageQuery.isError
          ? `检查失败：/storage/configured - ${getErrorMessage(storageQuery.error)}`
          : `BOS ${storageQuery.data ? '已配置' : '未配置'}，上传素材前请确认存储桶和密钥可用。`,
    },
    {
      title: '经营 KPI',
      source: '/dashboard/kpi-unified',
      status: kpiQuery.isPending ? 'checking' : kpiQuery.isError ? 'error' : 'ok',
      description: kpiQuery.isPending
        ? '正在检查统一 KPI 接口...'
        : kpiQuery.isError
          ? `检查失败：/dashboard/kpi-unified - ${getErrorMessage(kpiQuery.error)}`
          : `已接入 /dashboard/kpi-unified，最近 7 天 GMV ¥${Number(kpiQuery.data?.gmvToday ?? 0).toLocaleString()}。`,
    },
  ] as const

  const dependencyErrors = healthCards.filter((card) => card.status === 'error').length
  const dependencyWarnings = healthCards.filter((card) => card.status === 'warn').length
  const dependencyChecking = healthCards.filter((card) => card.status === 'checking').length
  const readinessLabel = dependencyChecking > 0
    ? `${dependencyChecking} 项检查中`
    : dependencyErrors > 0
      ? `${dependencyErrors} 项异常`
      : dependencyWarnings > 0
        ? `${dependencyWarnings} 项待配置`
        : '基础依赖已连接'

  const handleRecheck = () => {
    void productsQuery.refetch()
    void modelsQuery.refetch()
    void storageQuery.refetch()
    void kpiQuery.refetch()
  }

  return (
    <Box
      sx={{ p: 3, maxWidth: 920, mx: 'auto' }}
      data-testid="onboarding-workbench"
      data-contract-scope="admin-onboarding-dependency-readiness"
      data-ready-endpoints={ONBOARDING_READY_ENDPOINTS}
      data-navigation-targets={ONBOARDING_NAVIGATION_TARGETS}
      data-unsupported-actions={ONBOARDING_UNSUPPORTED_ACTIONS}
      data-active-step={activeStep}
      data-dependency-errors={dependencyErrors}
      data-dependency-warnings={dependencyWarnings}
      data-dependency-checking={dependencyChecking}
      data-readiness-label={readinessLabel}
      data-no-local-readiness-complete="true"
      data-no-local-dependency-fallback="true"
      data-navigation-only="true"
    >
      <PageHeader
        title="初始化引导"
        subtitle="按真实业务依赖完成账号、商品、直播、短视频和 AI 模型的基础配置。"
        breadcrumbs={[{ label: '系统' }, { label: '初始化' }]}
        actions={(
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRecheck}>
            重新检查依赖
          </Button>
        )}
      />

      <Alert severity={dependencyErrors > 0 ? 'warning' : 'info'} variant="outlined" sx={{ mb: 3 }}>
        依赖状态：{readinessLabel}。本页只负责导航和流程提示，不会伪造初始化完成状态；各步骤是否可用以后端页面真实接口、权限和数据源状态为准。
      </Alert>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {healthCards.map((card) => (
          <Grid item xs={12} sm={6} md={3} key={card.title}>
            <Card
              variant="outlined"
              data-testid="onboarding-dependency-card"
              data-contract-source={card.source}
              data-dependency-title={card.title}
              data-dependency-status={card.status}
              data-no-local-dependency-fallback="true"
            >
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 1 }}>
                  <Typography variant="subtitle2" fontWeight={700}>{card.title}</Typography>
                  <Chip
                    size="small"
                    label={card.status === 'checking' ? '检查中' : card.status === 'error' ? '异常' : card.status === 'warn' ? '待配置' : '已连接'}
                    color={card.status === 'error' ? 'error' : card.status === 'warn' ? 'warning' : card.status === 'ok' ? 'success' : 'default'}
                    variant={card.status === 'checking' ? 'outlined' : 'filled'}
                  />
                </Box>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>{card.source}</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>{card.description}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Stepper activeStep={activeStep} orientation="vertical">
        {STEPS.map((step, index) => (
          <Step
            key={step.label}
            data-testid="onboarding-step-contract"
            data-step-index={index}
            data-navigation-target={step.path}
            data-navigation-only="true"
            data-no-local-readiness-complete="true"
          >
            <StepLabel icon={step.icon}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>{step.label}</Typography>
            </StepLabel>
            <StepContent>
              <Paper variant="outlined" sx={{ p: 2.5, mb: 2 }}>
                <Typography variant="body2" color="text.secondary">{step.description}</Typography>
              </Paper>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button variant="contained" onClick={() => handleGo(step.path)}>
                  {step.action}
                </Button>
                <Button variant="outlined" onClick={handleNext}>
                  {index === STEPS.length - 1 ? '完成浏览' : '稍后完成'}
                </Button>
                {index > 0 && (
                  <Button onClick={handleBack}>上一步</Button>
                )}
              </Box>
            </StepContent>
          </Step>
        ))}
      </Stepper>

      {activeStep === STEPS.length && (
        <Paper
          sx={{ p: 3, mt: 2, textAlign: 'center' }}
          data-testid="onboarding-completion-panel"
          data-no-local-readiness-complete="true"
          data-readiness-label={readinessLabel}
        >
          <Typography variant="h6" sx={{ mb: 1 }}>引导已浏览</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            初始化是否真正完成，请以账号、商品、直播、短视频和 AI 配置页面的真实状态为准；当前依赖状态为：{readinessLabel}。
          </Typography>
          <Button variant="contained" onClick={() => navigate('/admin/dashboard')}>
            进入控制台
          </Button>
        </Paper>
      )}
    </Box>
  )
}
