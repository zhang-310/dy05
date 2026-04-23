import { memo, useState, useEffect, useRef, useCallback } from 'react'
import { Box, Drawer, Paper, Typography } from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useToast } from '@/contexts/ToastContext'
import { FlowProgressHeader } from './generation/FlowProgressHeader'
import { FlowStepList } from './generation/FlowStepList'
import { FlowActionBar } from './generation/FlowActionBar'
import type { FlowStep, GroupedSteps } from './generation/types'

// Re-export types so existing imports keep working
export type { FlowStep } from './generation/types'
export interface GenerationFlowPanelProps {
  variant?: 'drawer' | 'inline'
  open: boolean
  steps: FlowStep[]
  currentLabel?: string
  total?: number
  estimatedTotalSeconds?: number
  modelName?: string
  onCancel?: () => void
  onClose?: () => void
  onDismiss?: () => void
  isGenerating?: boolean
  onRetryStep?: (step: FlowStep) => void
  onRetryAllFailed?: () => void
  onSaveToLibrary?: () => void
  onExport?: () => void
  saveLibLoading?: boolean
  exportLoading?: boolean
  justCompleted?: boolean
  sseReconnecting?: boolean
  cancellingGen?: boolean
  groupedSteps?: GroupedSteps[]
  /** P1-03: 生成开始时间戳（ms），用于计算预估剩余时间 */
  genStartTimeMs?: number
}

export const GenerationFlowPanel = memo(function GenerationFlowPanel({
  variant = 'drawer',
  open,
  steps,
  currentLabel,
  total = 0,
  estimatedTotalSeconds,
  modelName,
  onCancel,
  onClose,
  onDismiss,
  isGenerating,
  onRetryStep,
  onRetryAllFailed,
  onSaveToLibrary,
  onExport,
  saveLibLoading = false,
  exportLoading = false,
  justCompleted = false,
  cancellingGen = false,
  sseReconnecting = false,
  groupedSteps,
  genStartTimeMs,
}: GenerationFlowPanelProps) {
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set())
  const [groupAccordionExpanded, setGroupAccordionExpanded] = useState<Record<number, boolean>>({})
  const lastStepRef = useRef<HTMLDivElement>(null)
  const toast = useToast()

  useEffect(() => {
    if (!open || !onClose) return
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [open, onClose])

  useEffect(() => {
    if (steps.length === 0) return
    lastStepRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [steps.length, steps[steps.length - 1]?.status])

  useEffect(() => {
    if (steps.length === 0) return
    const lastStep = steps[steps.length - 1]
    if ((lastStep.status === 'done' || lastStep.status === 'failed') && (lastStep.content || lastStep.errorMsg)) {
      const key = lastStep.stepKey ?? `step-${steps.length - 1}`
      setExpandedKeys((prev) => {
        if (prev.has(key)) return prev
        return new Set(prev).add(key)
      })
    }
  }, [steps.length, steps[steps.length - 1]?.status])

  const toggleExpand = useCallback((key: string) => {
    setExpandedKeys((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }, [])

  const copyToClipboard = useCallback(
    (text: string) => {
      navigator.clipboard
        ?.writeText(text)
        .then(() => toast('已复制', 'success'))
        .catch((e) => console.error('复制失败:', e))
    },
    [toast],
  )

  const handleExpandAll = useCallback(() => {
    const allKeys = new Set<string>()
    steps.forEach((s, i) => {
      if ((s.status === 'done' || s.status === 'failed') && (s.content || s.errorMsg)) {
        allKeys.add(s.stepKey ?? `step-${i}`)
      }
    })
    setExpandedKeys(allKeys)
  }, [steps])

  const handleCollapseAll = useCallback(() => {
    setExpandedKeys(new Set())
  }, [])

  const handleGroupAccordionChange = useCallback((gi: number, next: boolean) => {
    setGroupAccordionExpanded((prev) => ({ ...prev, [gi]: next }))
  }, [])

  const isInline = variant === 'inline'

  // Empty state for inline variant
  if (isInline && steps.length === 0 && !currentLabel && !isGenerating) {
    return (
      <Box
        sx={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          p: 4,
          gap: 1.5,
        }}
      >
        <AutoAwesomeIcon sx={{ fontSize: 40, color: 'action.disabled' }} />
        <Typography variant="subtitle2" color="text.secondary" textAlign="center" fontWeight={600}>
          生成进度
        </Typography>
        <Typography
          variant="body2"
          color="text.disabled"
          textAlign="center"
          sx={{ maxWidth: 200 }}
        >
          点击左侧「一键生成」按钮开始，生成进度将在此实时显示
        </Typography>
      </Box>
    )
  }

  if (!isInline && !open) return null

  const panelContent = (
    <Paper
      elevation={0}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderRadius: isInline ? 1 : 0,
        ...(isInline ? {} : { borderLeft: '1px solid', borderColor: 'divider' }),
      }}
    >
      <FlowProgressHeader
        steps={steps}
        modelName={modelName}
        isGenerating={isGenerating}
        isInline={isInline}
        sseReconnecting={sseReconnecting}
        onClose={onClose}
        onRetryAllFailed={onRetryAllFailed}
        expandedKeys={expandedKeys}
        onExpandAll={handleExpandAll}
        onCollapseAll={handleCollapseAll}
        genStartTimeMs={genStartTimeMs}
      />

      <Box
        sx={{
          flex: 1,
          overflow: 'auto',
          py: 1.5,
          px: 1.5,
          display: 'flex',
          flexDirection: 'column',
          gap: 1.5,
        }}
      >
        <FlowStepList
          steps={steps}
          groupedSteps={groupedSteps}
          currentLabel={currentLabel}
          justCompleted={justCompleted}
          expandedKeys={expandedKeys}
          groupAccordionExpanded={groupAccordionExpanded}
          onToggleExpand={toggleExpand}
          onCopy={copyToClipboard}
          onRetryStep={onRetryStep}
          onGroupAccordionChange={handleGroupAccordionChange}
          lastStepRef={lastStepRef}
        />
      </Box>

      <FlowActionBar
        steps={steps}
        total={total}
        isGenerating={isGenerating}
        estimatedTotalSeconds={estimatedTotalSeconds}
        cancellingGen={cancellingGen}
        saveLibLoading={saveLibLoading}
        exportLoading={exportLoading}
        onCancel={onCancel}
        onClose={onClose}
        onDismiss={onDismiss}
        onSaveToLibrary={onSaveToLibrary}
        onExport={onExport}
      />

      <style>{`
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
        @keyframes checkPop {
          0% { transform: scale(0.6); opacity: 0.6; }
          60% { transform: scale(1.25); }
          100% { transform: scale(1); opacity: 1; }
        }
      `}</style>
    </Paper>
  )

  if (isInline) {
    return <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>{panelContent}</Box>
  }

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose ?? (() => {})}
      PaperProps={{ sx: { width: { xs: '100%', sm: 560 }, maxWidth: '100%' } }}
    >
      {panelContent}
    </Drawer>
  )
})
