import { useState } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Stack,
  Chip,
  Button,
  TextField,
  CircularProgress,
  LinearProgress,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import type { DigitalHumanGenerateResult } from '@/api/digital-human'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'

export default function DigitalHumanPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [scriptText, setScriptText] = useState('')
  const [voiceId, setVoiceId] = useState('zh-CN-XiaoxiaoNeural')
  const [avatarId, setAvatarId] = useState('default')
  const [result, setResult] = useState<DigitalHumanGenerateResult | null>(null)

  const { data: statusData, isLoading: statusLoading } = useQuery({
    queryKey: ['digital-human-status'],
    queryFn: () => aiApi.digitalHumanStatus(),
    refetchInterval: 30000,
  })

  const isAvailable = statusData?.available === true

  const generateMutation = useMutation({
    mutationFn: async (): Promise<DigitalHumanGenerateResult> =>
      aiApi.digitalHumanGenerate({
        scriptText,
        voiceId: voiceId.trim() || undefined,
        avatarId: avatarId.trim() || undefined,
      }),
    onSuccess: (data) => {
      setResult(data)
      if (data.success) {
        toast(data.message || '数字人视频生成成功', 'success')
      } else {
        toast(data.message || '生成未返回有效视频', 'warning')
      }
    },
    onError: (e: Error) => toast(e.message || '生成失败', 'error'),
  })

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 2 }}>
      <PageHeader
        title="数字人"
        subtitle="HeyGen 同步口播（/ai/digital-human）。六产品扣费轨请用 POST /api/v1/digital-human/create（见 api/digital-human-product.ts）。工作流异步任务走 SvDigitalHumanTask/Webhook。"
      />

      {statusLoading && <LinearProgress />}

      {/* 服务状态 */}
      <Card variant="outlined">
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Stack direction="row" spacing={2} alignItems="center">
              <Typography variant="subtitle2">数字人服务状态</Typography>
              <Chip
                label={isAvailable ? '可用' : '未配置或不可用'}
                size="small"
                color={isAvailable ? 'success' : 'warning'}
              />
            </Stack>
            <Button
              size="small"
              startIcon={<RefreshIcon />}
              onClick={() => void qc.invalidateQueries({ queryKey: ['digital-human-status'] })}
            >
              刷新状态
            </Button>
          </Stack>
          {statusData && (
            <Stack direction="row" spacing={3} sx={{ mt: 1.5 }} flexWrap="wrap">
              <Box>
                <Typography variant="caption" color="text.secondary" display="block">
                  available
                </Typography>
                <Typography variant="body2" fontWeight={500}>
                  {String(statusData.available)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" display="block">
                  provider
                </Typography>
                <Typography variant="body2" fontWeight={500}>
                  {statusData.provider ?? '—'}
                </Typography>
              </Box>
            </Stack>
          )}
        </CardContent>
      </Card>

      {/* 生成表单 */}
      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" mb={2}>
            生成数字人口播视频
          </Typography>
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="voiceId（音色）"
                value={voiceId}
                onChange={(e) => setVoiceId(e.target.value)}
                size="small"
                sx={{ minWidth: 260 }}
                helperText="如 zh-CN-XiaoxiaoNeural，与 HeyGen 配置一致"
              />
              <TextField
                label="avatarId（形象）"
                value={avatarId}
                onChange={(e) => setAvatarId(e.target.value)}
                size="small"
                sx={{ minWidth: 260 }}
                placeholder="default 或 HeyGen 控制台中的 Avatar ID"
                helperText="请填写 HeyGen 侧真实 avatar_id，勿依赖虚构下拉值"
              />
            </Stack>
            <TextField
              label="播报脚本 scriptText"
              value={scriptText}
              onChange={(e) => setScriptText(e.target.value)}
              multiline
              minRows={5}
              fullWidth
              size="small"
              placeholder="请输入数字人播报的文字内容..."
            />
            <Box>
              <Button
                variant="contained"
                onClick={() => generateMutation.mutate()}
                disabled={!scriptText.trim() || !isAvailable || generateMutation.isPending}
                startIcon={generateMutation.isPending ? <CircularProgress size={18} /> : undefined}
              >
                提交生成
              </Button>
              {!isAvailable && (
                <Typography variant="caption" color="warning.main" sx={{ ml: 2 }}>
                  服务未配置（available=false），请配置 HeyGen 等 Provider 后再试
                </Typography>
              )}
            </Box>
          </Stack>
        </CardContent>
      </Card>

      {/* 生成结果 */}
      {result && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>
              生成结果
            </Typography>
            {result.videoUrl ? (
              <Box
                component="video"
                controls
                src={result.videoUrl}
                sx={{ maxWidth: '100%', maxHeight: 400, display: 'block', borderRadius: 1 }}
              />
            ) : (
              <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', m: 0 }}>
                {JSON.stringify(result, null, 2)}
              </Box>
            )}
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
              {result.provider} · {result.message}
            </Typography>
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
