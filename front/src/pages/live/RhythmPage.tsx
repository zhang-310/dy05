import { useMemo, useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Divider, Grid, LinearProgress, Stack, TextField, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import DeleteIcon from '@mui/icons-material/Delete'
import SaveIcon from '@mui/icons-material/Save'
import { alpha } from '@mui/material/styles'
import { useMutation } from '@tanstack/react-query'
import { liveApi } from '@/api/live'
import { PageHeader } from '@/components/base/PageHeader'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'

interface RhythmSlot {
  scriptId: string
  sequenceNo: string
  durationLimitSec: string
}

const RHYTHM_READY_ENDPOINTS = {
  optimize: '/live/rhythm/optimize',
  save: '/live/rhythm/save-rhythm',
} as const

const RHYTHM_CONTEXT_ENDPOINTS = [
  RHYTHM_READY_ENDPOINTS.optimize,
  RHYTHM_READY_ENDPOINTS.save,
]

const RHYTHM_UNSUPPORTED_ACTIONS = [
  'rhythm-get-rhythm',
  'rhythm-suggest-legacy',
  'local-plan-fallback',
  'script-search-autofill',
  'product-strategy',
  'batch-order',
  'shortvideo-export',
]

function parseSlots(slots: RhythmSlot[]) {
  return slots
    .map((slot) => ({
      scriptId: Number(slot.scriptId),
      sequenceNo: Number(slot.sequenceNo),
      durationLimitSec: Number(slot.durationLimitSec),
    }))
    .filter((slot) => slot.scriptId > 0)
}

function slotContext(slots: Array<{ scriptId: number; sequenceNo: number; durationLimitSec: number }>) {
  return slots.map((slot) => `${slot.scriptId}:${slot.sequenceNo}/${slot.durationLimitSec}s`).join(',') || '空'
}

export default function RhythmPage() {
  const toast = useToast()
  const [sessionId, setSessionId] = useState('')
  const [slots, setSlots] = useState<RhythmSlot[]>([
    { scriptId: '', sequenceNo: '1', durationLimitSec: '60' },
  ])

  const parsedSessionId = Number(sessionId)
  const payloadSlots = useMemo(() => parseSlots(slots), [slots])
  const hasInvalidSlot = slots.some((slot) => {
    if (!slot.scriptId) return false
    return Number(slot.scriptId) <= 0 || Number(slot.sequenceNo) <= 0 || Number(slot.durationLimitSec) <= 0
  })

  const optimizeMut = useMutation({
    mutationFn: (sid: number) => liveApi.rhythmOptimize(sid),
    onSuccess: () => toast('节奏优化建议已生成', 'success'),
    onError: (error: Error) => toast(`${RHYTHM_READY_ENDPOINTS.optimize} ${getErrorMessage(error)}`, 'error'),
  })

  const saveMut = useMutation({
    mutationFn: () => liveApi.rhythmSave({ sessionId: parsedSessionId, slots: payloadSlots }),
    onSuccess: (result) => toast(`节奏槽位已保存：${String(result.updatedSlots ?? payloadSlots.length)} 条`, 'success'),
    onError: (error: Error) => toast(`${RHYTHM_READY_ENDPOINTS.save} ${getErrorMessage(error)}`, 'error'),
  })

  const addSlot = () => {
    setSlots((current) => [
      ...current,
      { scriptId: '', sequenceNo: String(current.length + 1), durationLimitSec: '60' },
    ])
  }

  const updateSlot = (index: number, field: keyof RhythmSlot, value: string) => {
    setSlots((current) => current.map((slot, i) => i === index ? { ...slot, [field]: value } : slot))
  }

  const removeSlot = (index: number) => {
    setSlots((current) => current.length === 1 ? current : current.filter((_, i) => i !== index))
  }

  const canSubmit = parsedSessionId > 0 && payloadSlots.length > 0 && !hasInvalidSlot && !saveMut.isPending
  const optimizeContext = `上下文：route=/admin/live/rhythm; sessionId=${parsedSessionId || '空'}`
  const saveContext = `上下文：route=/admin/live/rhythm; sessionId=${parsedSessionId || '空'}; slotCount=${payloadSlots.length}; slots=${slotContext(payloadSlots)}`

  return (
    <Box
      sx={{ py: 1 }}
      data-testid="live-rhythm-workbench"
      data-contract-scope="live-rhythm-slot-editor"
      data-ready-endpoints={Object.values(RHYTHM_READY_ENDPOINTS).join('|')}
      data-context-endpoints={RHYTHM_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={RHYTHM_UNSUPPORTED_ACTIONS.join('|')}
      data-session-id={parsedSessionId > 0 ? parsedSessionId : 0}
      data-slot-count={slots.length}
      data-valid-slot-count={payloadSlots.length}
      data-has-invalid-slot={String(hasInvalidSlot)}
    >
      <PageHeader
        title="直播节奏编排"
        subtitle="对齐真实 `/live/rhythm/optimize` 与 `/save-rhythm`；当前后端没有独立读取节奏方案接口。"
        breadcrumbs={[{ label: '直播' }, { label: '节奏编排' }]}
      />

      <Stack spacing={2}>
        <Alert
          severity="warning"
          data-testid="live-rhythm-contract-alert"
          data-contract-source={RHYTHM_CONTEXT_ENDPOINTS.join('|')}
          data-no-get-rhythm="true"
          data-no-suggest-legacy="true"
          data-no-local-plan-fallback="true"
          data-no-script-search-autofill="true"
          data-no-shortvideo-export="true"
        >
          当前控制器只提供优化建议和保存槽位。保存动作会更新该场次话术的 `sequenceNo` 与 `durationLimitSec`，不会单独生成可读取的节奏方案记录。
        </Alert>

        <Card
          variant="outlined"
          data-testid="live-rhythm-control-card"
          data-contract-source={RHYTHM_CONTEXT_ENDPOINTS.join('|')}
          data-optimize-source={RHYTHM_READY_ENDPOINTS.optimize}
          data-save-source={RHYTHM_READY_ENDPOINTS.save}
          data-no-read-before-edit="true"
        >
          <CardContent>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ xs: 'stretch', sm: 'center' }}>
              <TextField
                size="small"
                label="直播场次 ID"
                value={sessionId}
                onChange={(e) => setSessionId(e.target.value)}
                sx={{ width: { xs: '100%', sm: 180 } }}
                inputProps={{ inputMode: 'numeric' }}
              />
              <Button
                variant="outlined"
                startIcon={<AutoFixHighIcon />}
                disabled={parsedSessionId <= 0 || optimizeMut.isPending}
                onClick={() => optimizeMut.mutate(parsedSessionId)}
                data-testid="live-rhythm-optimize-button"
              >
                AI 优化建议
              </Button>
              <Button
                variant="contained"
                startIcon={<SaveIcon />}
                disabled={!canSubmit}
                onClick={() => saveMut.mutate()}
                data-testid="live-rhythm-save-button"
              >
                保存槽位
              </Button>
              {parsedSessionId > 0 && <Chip label={`场次 #${parsedSessionId}`} color="primary" variant="outlined" />}
            </Stack>
          </CardContent>
        </Card>

        {(optimizeMut.isPending || saveMut.isPending) && <LinearProgress sx={{ borderRadius: 1 }} />}

        {optimizeMut.isError && (
          <Alert
            severity="error"
            data-testid="live-rhythm-optimize-error"
            data-contract-source={RHYTHM_READY_ENDPOINTS.optimize}
            data-no-local-optimization-fallback="true"
            action={<Button color="inherit" size="small" onClick={() => optimizeMut.mutate(parsedSessionId)}>重试</Button>}
          >
            {RHYTHM_READY_ENDPOINTS.optimize} 节奏优化失败：{getErrorMessage(optimizeMut.error)}。{optimizeContext}
          </Alert>
        )}
        {saveMut.isError && (
          <Alert
            severity="error"
            data-testid="live-rhythm-save-error"
            data-contract-source={RHYTHM_READY_ENDPOINTS.save}
            data-no-local-save-fallback="true"
            data-input-retained="true"
          >
            {RHYTHM_READY_ENDPOINTS.save} 保存节奏失败：{getErrorMessage(saveMut.error)}。{saveContext}。失败会保留当前槽位输入，不会本地伪造保存成功。
          </Alert>
        )}
        {saveMut.data && (
          <Alert
            severity="success"
            data-testid="live-rhythm-save-success"
            data-contract-source={RHYTHM_READY_ENDPOINTS.save}
            data-updated-slots={String(saveMut.data.updatedSlots ?? payloadSlots.length)}
            data-no-local-save-fallback="true"
          >
            {RHYTHM_READY_ENDPOINTS.save} 已确认保存 {String(saveMut.data.updatedSlots ?? payloadSlots.length)} 个槽位。
          </Alert>
        )}

        {optimizeMut.data && (
          <Card
            variant="outlined"
            data-testid="live-rhythm-optimization-card"
            data-contract-source={RHYTHM_READY_ENDPOINTS.optimize}
            data-no-local-optimization-fallback="true"
          >
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1} mb={1}>
                <Typography variant="subtitle2">AI 优化返回</Typography>
                {optimizeMut.data.status != null && <Chip size="small" label={String(optimizeMut.data.status)} />}
              </Stack>
              {optimizeMut.data.message != null && (
                <Alert severity={optimizeMut.data.status === 'success' ? 'success' : 'info'} sx={{ mb: 1 }}>
                  {String(optimizeMut.data.message)}
                </Alert>
              )}
              <Box
                component="pre"
                data-testid="live-rhythm-optimization-surface"
                data-contract-source={RHYTHM_READY_ENDPOINTS.optimize}
                sx={(theme) => ({
                  fontSize: 12,
                  bgcolor: theme.palette.mode === 'dark'
                    ? theme.palette.background.default
                    : alpha(theme.palette.common.black, 0.025),
                  border: `1px solid ${theme.palette.divider}`,
                  p: 2,
                  borderRadius: 1,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                })}
              >
                {String(optimizeMut.data.optimization ?? JSON.stringify(optimizeMut.data, null, 2))}
              </Box>
            </CardContent>
          </Card>
        )}

        <Card
          variant="outlined"
          data-testid="live-rhythm-slot-editor"
          data-contract-source={RHYTHM_READY_ENDPOINTS.save}
          data-slot-count={slots.length}
          data-valid-slot-count={payloadSlots.length}
          data-no-extra-fields="true"
        >
          <CardContent>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'stretch', sm: 'center' }} spacing={1} mb={2}>
              <Box>
                <Typography variant="h6">槽位编排</Typography>
                <Typography variant="body2" color="text.secondary">
                  只提交后端消费的 `scriptId / sequenceNo / durationLimitSec`，避免保存无效字段。
                </Typography>
              </Box>
              <Button size="small" startIcon={<AddIcon />} onClick={addSlot}>新增槽位</Button>
            </Stack>
            <Divider sx={{ mb: 2 }} />

            {hasInvalidSlot && (
              <Alert
                severity="error"
                sx={{ mb: 2 }}
                data-testid="live-rhythm-validation-error"
                data-no-local-plan-fallback="true"
              >
                已填写的话术 ID、顺序、时长必须是大于 0 的数字。
              </Alert>
            )}

            <Grid container spacing={2}>
              {slots.map((slot, index) => (
                <Grid item xs={12} md={4} key={index}>
                  <Card
                    variant="outlined"
                    sx={{ height: '100%' }}
                    data-testid="live-rhythm-slot-card"
                    data-slot-index={index}
                    data-script-id={slot.scriptId || 'empty'}
                    data-sequence-no={slot.sequenceNo || 'empty'}
                    data-duration-limit-sec={slot.durationLimitSec || 'empty'}
                  >
                    <CardContent>
                      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="subtitle2">槽位 {index + 1}</Typography>
                        <Button size="small" color="error" startIcon={<DeleteIcon />} disabled={slots.length === 1} onClick={() => removeSlot(index)}>
                          删除
                        </Button>
                      </Stack>
                      <Stack spacing={1.5}>
                        <TextField
                          size="small"
                          label="话术 ID"
                          value={slot.scriptId}
                          onChange={(e) => updateSlot(index, 'scriptId', e.target.value)}
                          inputProps={{ inputMode: 'numeric' }}
                        />
                        <TextField
                          size="small"
                          label="顺序"
                          value={slot.sequenceNo}
                          onChange={(e) => updateSlot(index, 'sequenceNo', e.target.value)}
                          inputProps={{ inputMode: 'numeric' }}
                        />
                        <TextField
                          size="small"
                          label="时长上限（秒）"
                          value={slot.durationLimitSec}
                          onChange={(e) => updateSlot(index, 'durationLimitSec', e.target.value)}
                          inputProps={{ inputMode: 'numeric' }}
                        />
                      </Stack>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      </Stack>
    </Box>
  )
}
