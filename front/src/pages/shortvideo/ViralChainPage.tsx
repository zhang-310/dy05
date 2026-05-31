import { useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import TaskAltIcon from '@mui/icons-material/TaskAlt'
import AccountTreeIcon from '@mui/icons-material/AccountTree'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

const CAPABILITY_CARDS = [
  {
    label: '当前可执行',
    value: 'AI 分析任务',
    detail: '`/short-video/viral/analyze` 写入/刷新单条爆款的分析结果。',
    color: 'success' as const,
  },
  {
    label: '可查看结果',
    value: '爆款库详情',
    detail: '分析完成后到爆款库详情查看口播、场景、结构和二创变量。',
    color: 'primary' as const,
  },
  {
    label: '明确降级',
    value: '传播链图谱未落库',
    detail: '后端没有节点/边查询接口，本页不渲染假图谱。',
    color: 'warning' as const,
  },
]

const VIRAL_CHAIN_ENDPOINTS = {
  analyze: '/short-video/viral/analyze',
} as const
const VIRAL_CHAIN_READY_ENDPOINTS = VIRAL_CHAIN_ENDPOINTS.analyze
const VIRAL_CHAIN_UNSUPPORTED_ENDPOINTS = [
  '/short-video/viral-chain/mock',
  '/short-video/viral-chain/local-graph',
  '/short-video/viral-chain/static-graph',
  '/short-video/viral-chain/nodes',
  '/short-video/viral-chain/edges',
  '/short-video/viral-chain/export',
  '/short-video/viral-chain/local-node-synthesis',
  '/short-video/viral/local-analyze',
  '/short-video/viral-remake/assign-task',
].join('|')
const VIRAL_CHAIN_SUPPORTED_ACTIONS = [
  'submit-viral-analysis',
  'navigate-viral-library',
].join('|')

export default function ViralChainPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const [viralVideoId, setViralVideoId] = useState('')
  const [lastSubmittedId, setLastSubmittedId] = useState<number | null>(null)

  const analyzeMut = useMutation({
    mutationFn: () => shortvideoApi.viralAnalyze(Number(viralVideoId)),
    onSuccess: (res) => {
      const id = Number(res?.id ?? viralVideoId)
      setLastSubmittedId(Number.isFinite(id) ? id : null)
      toast('爆款 AI 分析任务已提交', 'success')
    },
    onError: (e) => toast(`触发分析失败：${getErrorMessage(e)}`, 'error'),
  })

  const normalizedId = Number(viralVideoId)
  const invalidId = viralVideoId.trim() !== '' && (!Number.isFinite(normalizedId) || normalizedId <= 0)

  return (
    <Box
      data-testid="viral-chain-page"
      data-ready-endpoints={VIRAL_CHAIN_READY_ENDPOINTS}
      data-unsupported-endpoints={VIRAL_CHAIN_UNSUPPORTED_ENDPOINTS}
      data-supported-actions={VIRAL_CHAIN_SUPPORTED_ACTIONS}
      data-ready-routes={shortvideoRoutes.viralVideos}
      data-input-video-id={viralVideoId.trim()}
      data-last-submitted-id={lastSubmittedId ?? ''}
      data-analysis-pending={analyzeMut.isPending ? 'true' : 'false'}
      data-no-local-graph-fallback="true"
      data-no-graph-node-synthesis="true"
      data-no-shooting-task-assignment="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="爆款传播链分析"
        breadcrumbs={[{ label: '短视频' }, { label: '传播链' }]}
        subtitle="当前后端只提供单条爆款 AI 分析触发与详情查询，传播链图谱节点/边接口尚未落库。"
        actions={
          <Button
            variant="outlined"
            startIcon={<OpenInNewIcon />}
            onClick={() => navigate(shortvideoRoutes.viralVideos)}
            data-testid="viral-chain-open-library-button"
            data-target-route={shortvideoRoutes.viralVideos}
          >
            打开爆款库
          </Button>
        }
      />

      <Alert
        severity="warning"
        variant="outlined"
        data-testid="viral-chain-boundary-contract"
        data-ready-endpoint={VIRAL_CHAIN_ENDPOINTS.analyze}
        data-unsupported-endpoints={VIRAL_CHAIN_UNSUPPORTED_ENDPOINTS}
        data-no-local-graph-fallback="true"
        data-no-graph-node-synthesis="true"
        data-no-shooting-task-assignment="true"
      >
        本页已收敛为真实可用链路：只调用 POST {VIRAL_CHAIN_ENDPOINTS.analyze} 提交分析任务；传播节点数、层级、改编关系等图谱能力需要后端新增传播链表和查询接口后再启用。
      </Alert>

      <Grid container spacing={2} data-testid="viral-chain-capability-contract" data-no-local-graph-fallback="true">
        {CAPABILITY_CARDS.map((item) => (
          <Grid item xs={12} md={4} key={item.label}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardContent>
                <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
                  <Chip label={item.label} size="small" color={item.color} variant="outlined" />
                </Stack>
                <Typography variant="h6" fontWeight={700}>{item.value}</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  {item.detail}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Card variant="outlined" data-testid="viral-chain-analyze-card" data-ready-endpoint={VIRAL_CHAIN_ENDPOINTS.analyze}>
        <CardContent>
          <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
            <SearchIcon color="primary" />
            <Typography variant="subtitle1" fontWeight={700}>提交爆款分析</Typography>
          </Stack>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            输入爆款库内主键 ID。提交后后台会复用爆款深度拆解服务，结果在爆款库详情页查看。
          </Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
            <TextField
              size="small"
              label="爆款视频 ID（库内）"
              value={viralVideoId}
              onChange={(e) => setViralVideoId(e.target.value)}
              placeholder="例如 18"
              error={invalidId}
              helperText={invalidId ? '请输入大于 0 的数字 ID' : '来自爆款库列表的 id 字段'}
              sx={{ flex: 1 }}
            />
            <Button
              variant="contained"
              startIcon={<SearchIcon />}
              onClick={() => analyzeMut.mutate()}
              disabled={!viralVideoId.trim() || invalidId || analyzeMut.isPending}
              data-testid="viral-chain-submit-button"
              data-contract-source={VIRAL_CHAIN_ENDPOINTS.analyze}
              data-disabled-reason={!viralVideoId.trim() ? 'empty-id' : invalidId ? 'invalid-id' : analyzeMut.isPending ? 'pending' : 'ready'}
            >
              {analyzeMut.isPending ? '提交中...' : '触发分析'}
            </Button>
          </Stack>

          {analyzeMut.isError && (
            <Alert
              severity="error"
              data-testid="viral-chain-analyze-error"
              data-input-retained="true"
              data-no-local-graph-fallback="true"
              sx={{ mt: 2 }}
            >
              分析任务提交失败（POST {VIRAL_CHAIN_ENDPOINTS.analyze}）：{getErrorMessage(analyzeMut.error)}。已保留爆款视频 ID，不会伪造传播链节点。
            </Alert>
          )}

          {lastSubmittedId != null && (
            <Alert
              severity="success"
              data-testid="viral-chain-submit-success"
              data-result-source="analyze-endpoint"
              data-no-local-graph-fallback="true"
              sx={{ mt: 2 }}
              action={
                <Button
                  color="inherit"
                  size="small"
                  onClick={() => navigate(`${shortvideoRoutes.viralVideos}?videoId=${lastSubmittedId}`)}
                  data-testid="viral-chain-view-detail-button"
                  data-target-route={`${shortvideoRoutes.viralVideos}?videoId=${lastSubmittedId}`}
                >
                  查看详情
                </Button>
              }
            >
              爆款 #{lastSubmittedId} 的 AI 分析任务已提交。若任务仍在处理中，请稍后在爆款库详情刷新查看。
            </Alert>
          )}
        </CardContent>
      </Card>

      <Card variant="outlined" data-testid="viral-chain-graph-downgrade" data-no-local-graph-fallback="true" data-no-graph-node-synthesis="true">
        <CardContent>
          <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
            <AccountTreeIcon color="disabled" />
            <Typography variant="subtitle1" fontWeight={700}>传播链图谱降级说明</Typography>
          </Stack>
          <Stack spacing={1.5}>
            <Stack direction="row" spacing={1} alignItems="center">
              <TaskAltIcon color="success" fontSize="small" />
              <Typography variant="body2">已具备：爆款库列表、详情、收藏、单条 AI 分析、深度拆解状态查询。</Typography>
            </Stack>
            <Divider />
            <Typography variant="body2" color="text.secondary">
              未具备：传播源视频、二创视频、父子边、传播层级和传播指标的后端表与接口。因此页面不再显示空图表或本地虚拟节点，避免误判系统已经完成传播链能力。
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  )
}
