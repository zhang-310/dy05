import { memo, forwardRef } from 'react'
import {
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Box,
  Button,
  LinearProgress,
  Paper,
  Tooltip,
  Typography,
  IconButton,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked'
import AutorenewIcon from '@mui/icons-material/Autorenew'
import ErrorIcon from '@mui/icons-material/Error'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import VerifiedIcon from '@mui/icons-material/Verified'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import GppBadIcon from '@mui/icons-material/GppBad'
import type { FlowStep, GroupedSteps } from './types'
import { getStepTitle, getStepDescription } from './utils'

// ── Grouped step item (inside Accordion) ──────────────────────────────────

interface GroupedStepItemProps {
  step: FlowStep
  onRetryStep?: (step: FlowStep) => void
}

const GroupedStepItem = memo(function GroupedStepItem({ step, onRetryStep }: GroupedStepItemProps) {
  return (
    <Paper
      variant="outlined"
      data-testid={step.status === 'failed' ? 'flow-step-grouped-failed-surface' : 'flow-step-grouped-surface'}
      data-contract-scope="live-generation-flow-grouped-step"
      data-contract-source="groupedSteps-prop"
      data-step-key={step.stepKey ?? ''}
      data-script-id={step.scriptId ?? ''}
      data-step-status={step.status}
      data-no-local-step-fallback="true"
      sx={(theme) => ({
        p: 1.5,
        borderRadius: 1,
        bgcolor:
          step.status === 'loading'
            ? 'action.hover'
            : step.status === 'failed'
              ? alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.16 : 0.08)
              : 'background.paper',
        borderColor: step.status === 'failed'
          ? alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.45 : 0.28)
          : 'divider',
      })}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        {step.status === 'done' && <CheckCircleIcon sx={{ fontSize: 18, color: 'success.main' }} />}
        {step.status === 'failed' && <ErrorIcon sx={{ fontSize: 18, color: 'error.main' }} />}
        {step.status === 'loading' && (
          <AutorenewIcon sx={{ fontSize: 18, color: 'primary.main', animation: 'spin 1s linear infinite' }} />
        )}
        {step.status === 'pending' && <RadioButtonUncheckedIcon sx={{ fontSize: 18, color: 'action.disabled' }} />}
        <Typography variant="body2" sx={{ flex: 1 }}>
          {getStepTitle(step.label)}
        </Typography>
        {step.status === 'failed' && onRetryStep && (
          <Button
            size="small"
            onClick={() => onRetryStep(step)}
            data-testid="flow-step-grouped-retry-button"
            data-contract-source="onRetryStep-prop"
            data-step-key={step.stepKey ?? ''}
          >
            重试
          </Button>
        )}
      </Box>
    </Paper>
  )
})

// ── Flat step item (timeline view) ────────────────────────────────────────

interface FlatStepItemProps {
  step: FlowStep
  idx: number
  isLast: boolean
  justCompleted: boolean
  expandedKeys: Set<string>
  onToggleExpand: (key: string) => void
  onCopy: (text: string) => void
  onRetryStep?: (step: FlowStep) => void
}

const FlatStepItem = memo(
  forwardRef<HTMLDivElement, FlatStepItemProps>(function FlatStepItem(
    { step, idx, isLast, justCompleted, expandedKeys, onToggleExpand, onCopy, onRetryStep },
    ref,
  ) {
    const stepKey = step.stepKey ?? `step-${idx}`
    const isExpanded = expandedKeys.has(stepKey)
    const contentText = step.status === 'failed' ? step.errorMsg : step.content
    const hasExpandableContent = (step.status === 'done' || step.status === 'failed') && (step.content || step.errorMsg)

    return (
      <Box sx={{ position: 'relative' }}>
        {/* Connector line */}
        {idx > 0 && (
          <Box
            sx={{
              position: 'absolute',
              top: -12,
              left: 15,
              width: 2,
              height: 12,
              bgcolor: step.status === 'loading' ? 'primary.main' : 'divider',
              ...(step.status === 'loading'
                ? {
                    animation: 'pulseLine 1.5s ease-in-out infinite',
                    '@keyframes pulseLine': {
                      '0%, 100%': { opacity: 0.4 },
                      '50%': { opacity: 1 },
                    },
                  }
                : {}),
            }}
          />
        )}
        <Paper
          ref={isLast ? ref : undefined}
          variant="outlined"
          data-testid={step.status === 'failed' ? 'flow-step-flat-failed-surface' : 'flow-step-flat-surface'}
          data-contract-scope="live-generation-flow-flat-step"
          data-contract-source="steps-prop"
          data-step-key={stepKey}
          data-script-id={step.scriptId ?? ''}
          data-step-status={step.status}
          data-expanded={isExpanded ? 'true' : 'false'}
          data-has-expandable-content={hasExpandableContent ? 'true' : 'false'}
          data-quality-score={step.qualityScore ?? ''}
          data-rag-ref-count={step.ragRefs?.length ?? 0}
          data-no-local-step-fallback="true"
          sx={(theme) => ({
            p: 1,
            borderRadius: 1.5,
            bgcolor:
              step.status === 'loading'
                ? 'action.hover'
                : step.status === 'failed'
                  ? alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.14 : 0.06)
                  : 'background.paper',
            borderColor:
              step.status === 'loading'
                ? 'primary.light'
                : step.status === 'failed'
                  ? alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.5 : 0.32)
                  : 'divider',
          })}
        >
          <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
            {/* Status icon */}
            <Box sx={{ mt: 0.125, flexShrink: 0 }}>
              {step.status === 'done' && (
                <CheckCircleIcon
                  sx={{
                    fontSize: 18,
                    color: 'success.main',
                    ...(justCompleted && isLast ? { animation: 'checkPop 0.3s ease-out' } : {}),
                  }}
                />
              )}
              {step.status === 'failed' && <ErrorIcon sx={{ fontSize: 18, color: 'error.main' }} />}
              {step.status === 'loading' && (
                <AutorenewIcon sx={{ fontSize: 16, color: 'primary.main', animation: 'spin 1s linear infinite' }} />
              )}
              {step.status === 'pending' && (
                <RadioButtonUncheckedIcon sx={{ fontSize: 18, color: 'action.disabled' }} />
              )}
            </Box>

            {/* Step content */}
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}>
                <Typography
                  variant="body2"
                  fontWeight={600}
                  sx={{
                    fontSize: 12,
                    color: step.status === 'failed' ? 'error.main' : 'text.primary',
                    flexShrink: 0,
                  }}
                >
                  {getStepTitle(step.label)}
                </Typography>

                {/* Duration */}
                {(step.status === 'done' || step.status === 'failed') && step.startTime && step.endTime && (
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{ fontWeight: 400, fontSize: 11, flexShrink: 0 }}
                  >
                    ({((step.endTime - step.startTime) / 1000).toFixed(1)}s)
                  </Typography>
                )}

                {/* Confidence badge */}
                {step.status === 'done' && step.confidence != null && (
                  <Tooltip
                    title={
                      step.confidence < 60
                        ? '置信度较低，建议人工复核'
                        : `AI 置信度 ${step.confidence}%`
                    }
                  >
                    <Typography
                      component="span"
                      variant="caption"
                      sx={{
                        fontWeight: 500,
                        fontSize: 11,
                        flexShrink: 0,
                        color:
                          step.confidence >= 80
                            ? 'success.main'
                            : step.confidence >= 60
                              ? 'warning.main'
                              : 'error.main',
                      }}
                    >
                      {step.confidence}%
                    </Typography>
                  </Tooltip>
                )}

                {/* Quality badges */}
                {step.qualityScore === 'checking' && (
                  <Tooltip title="质检中...">
                    <AutorenewIcon sx={{ fontSize: 14, color: 'primary.main', animation: 'spin 1s linear infinite' }} />
                  </Tooltip>
                )}
                {step.qualityScore === 'pass' && (
                  <Tooltip title="质检通过">
                    <VerifiedIcon sx={{ fontSize: 14, color: 'success.main' }} />
                  </Tooltip>
                )}
                {step.qualityScore === 'warning' && (
                  <Tooltip title={step.qualityIssues?.join('; ') ?? '存在警告'}>
                    <WarningAmberIcon sx={{ fontSize: 14, color: 'warning.main' }} />
                  </Tooltip>
                )}
                {step.qualityScore === 'fail' && (
                  <Tooltip title={step.qualityIssues?.join('; ') ?? '质检不通过'}>
                    <GppBadIcon sx={{ fontSize: 14, color: 'error.main' }} />
                  </Tooltip>
                )}

                {/* Expand/copy/retry inline buttons */}
                {hasExpandableContent && (
                  <Box sx={{ display: 'flex', alignItems: 'center', ml: 'auto', gap: 0, flexShrink: 0 }}>
                    {step.status === 'failed' && onRetryStep && (
                      <IconButton
                        size="small"
                        onClick={() => onRetryStep(step)}
                        title="重试"
                        sx={{ p: 0.25 }}
                        data-testid="flow-step-flat-retry-button"
                        data-contract-source="onRetryStep-prop"
                        data-step-key={stepKey}
                      >
                        <AutorenewIcon sx={{ fontSize: 14, color: 'primary.main' }} />
                      </IconButton>
                    )}
                    <IconButton
                      size="small"
                      onClick={() => onToggleExpand(stepKey)}
                      title={isExpanded ? '收起' : '展开'}
                      sx={{ p: 0.25 }}
                      data-testid="flow-step-flat-toggle-button"
                      data-contract-source="onToggleExpand-prop"
                      data-step-key={stepKey}
                    >
                      {isExpanded ? (
                        <ExpandLessIcon sx={{ fontSize: 16 }} />
                      ) : (
                        <ExpandMoreIcon sx={{ fontSize: 16 }} />
                      )}
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => onCopy(contentText ?? '')}
                      title="复制"
                      sx={{ p: 0.25 }}
                      data-testid="flow-step-flat-copy-button"
                      data-contract-source="browser-clipboard-prop"
                      data-action-owner="onCopy-prop"
                      data-step-key={stepKey}
                    >
                      <ContentCopyIcon sx={{ fontSize: 14 }} />
                    </IconButton>
                  </Box>
                )}
              </Box>

              {/* Step description */}
              {getStepDescription(step.label) && (step.status === 'loading' || step.status === 'done') && (
                <Typography
                  variant="caption"
                  sx={{ display: 'block', mt: 0.125, color: 'text.secondary', fontSize: 11 }}
                >
                  {getStepDescription(step.label)}
                </Typography>
              )}

              {/* Loading dots */}
              {step.status === 'loading' && !step.streamingContent && (
                <Typography
                  variant="caption"
                  sx={{ display: 'block', mt: 0.25, color: 'primary.main', fontWeight: 500, fontSize: 11 }}
                >
                  <Box
                    component="span"
                    sx={{
                      animation: 'typingDots 1.4s infinite',
                      '@keyframes typingDots': {
                        '0%': { content: '"."' },
                        '33%': { content: '".."' },
                        '66%': { content: '"..."' },
                      },
                    }}
                  >
                    ...
                  </Box>
                </Typography>
              )}

              {/* Streaming content */}
              {step.status === 'loading' && step.streamingContent && (
                <Typography
                  variant="body2"
                  sx={{
                    display: 'block',
                    mt: 0.25,
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                    color: 'text.secondary',
                    fontSize: 12,
                    lineHeight: 1.6,
                    maxHeight: 160,
                    overflow: 'auto',
                  }}
                >
                  {step.streamingContent}
                  <Box
                    component="span"
                    sx={{
                      display: 'inline-block',
                      width: 2,
                      height: '1em',
                      bgcolor: 'primary.main',
                      ml: 0.25,
                      animation: 'blink 0.8s step-end infinite',
                      '@keyframes blink': { '50%': { opacity: 0 } },
                    }}
                  />
                </Typography>
              )}
            </Box>
          </Box>

          {/* Expanded content area */}
          {hasExpandableContent && (
            <Box
              data-testid="flow-step-expanded-content"
              data-contract-source="steps-prop"
              data-no-local-step-fallback="true"
              sx={{ mt: 0.5, pt: 0.5, borderTop: 1, borderColor: 'divider' }}
            >
              <Typography
                variant="body2"
                sx={{
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  fontSize: 12,
                  lineHeight: 1.6,
                  ...(isExpanded
                    ? {}
                    : {
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                      }),
                  color: step.status === 'failed' ? 'error.main' : 'text.primary',
                }}
              >
                {contentText}
              </Typography>
              {step.status === 'failed' && (
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: 'block', mt: 0.5, fontSize: 11 }}
                >
                  建议：检查网络连接，或切换 AI 模型后点击「重试」
                </Typography>
              )}
              {step.ragRefs && step.ragRefs.length > 0 && (
                <Box sx={{ mt: 1, pt: 1, borderTop: 1, borderColor: 'divider' }}>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    fontWeight={500}
                    sx={{ display: 'block', mb: 0.5 }}
                  >
                    参考来源 ({step.ragRefs.length})
                  </Typography>
                  {step.ragRefs.map((ragRef, ri) => (
                    <Box key={ri} sx={{ display: 'flex', alignItems: 'baseline', gap: 0.5, mb: 0.25 }}>
                      <Typography variant="caption" color="primary.main" sx={{ fontWeight: 500 }}>
                        [{ri + 1}]
                      </Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>
                        {ragRef.docTitle ?? '未知文档'}
                        {ragRef.score != null && (
                          <Box component="span" sx={{ ml: 0.5, color: 'text.disabled' }}>
                            ({Math.round(ragRef.score * 100)}%)
                          </Box>
                        )}
                      </Typography>
                    </Box>
                  ))}
                </Box>
              )}
            </Box>
          )}
        </Paper>
      </Box>
    )
  }),
)

// ── Main FlowStepList component ───────────────────────────────────────────

export interface FlowStepListProps {
  steps: FlowStep[]
  groupedSteps?: GroupedSteps[]
  currentLabel?: string
  justCompleted: boolean
  expandedKeys: Set<string>
  groupAccordionExpanded: Record<number, boolean>
  onToggleExpand: (key: string) => void
  onCopy: (text: string) => void
  onRetryStep?: (step: FlowStep) => void
  onGroupAccordionChange: (gi: number, next: boolean) => void
  lastStepRef: React.Ref<HTMLDivElement>
}

export const FlowStepList = memo(function FlowStepList({
  steps,
  groupedSteps,
  currentLabel,
  justCompleted,
  expandedKeys,
  groupAccordionExpanded,
  onToggleExpand,
  onCopy,
  onRetryStep,
  onGroupAccordionChange,
  lastStepRef,
}: FlowStepListProps) {
  // ── Grouped (Accordion) view ──
  if (groupedSteps && groupedSteps.length > 0) {
    return (
      <>
        {groupedSteps.map((group, gi) => {
          const doneCount = group.steps.filter((s) => s.status === 'done').length
          const failedCount = group.steps.filter((s) => s.status === 'failed').length
          const pct = group.steps.length > 0 ? Math.round((doneCount / group.steps.length) * 100) : 0
          const needsAttention = group.steps.some((s) => s.status === 'loading' || s.status === 'failed')
          const accordionExpanded = needsAttention || groupAccordionExpanded[gi] === true

          return (
            <Accordion
              key={`${group.groupLabel}-${gi}`}
              expanded={accordionExpanded}
              onChange={(_, next) => {
                if (!needsAttention) {
                  onGroupAccordionChange(gi, next)
                }
              }}
            >
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flex: 1 }}>
                  <Typography variant="subtitle2">{group.groupLabel}</Typography>
                  <LinearProgress
                    variant="determinate"
                    value={pct}
                    sx={{ flex: 1, height: 4, borderRadius: 2, mx: 1 }}
                  />
                  <Typography variant="caption" color="text.secondary">
                    {doneCount}/{group.steps.length}
                    {failedCount > 0 && ` (${failedCount} 失败)`}
                  </Typography>
                </Box>
              </AccordionSummary>
              <AccordionDetails sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, pt: 0 }}>
                {group.steps.map((step, idx) => (
                  <GroupedStepItem
                    key={step.stepKey ?? `g${gi}-step-${idx}`}
                    step={step}
                    onRetryStep={onRetryStep}
                  />
                ))}
              </AccordionDetails>
            </Accordion>
          )
        })}
      </>
    )
  }

  // ── Flat timeline view ──
  const filteredSteps = steps.filter((step) => {
    if (step.status === 'pending' && !step.scriptId && !step.content && /^步骤\s*\d+$/.test(step.label))
      return false
    return true
  })

  return (
    <>
      {steps.length === 0 && currentLabel && (
        <Box
          data-testid="flow-step-current-label-only"
          data-contract-source="/live/ai/generate-full-pipelined-sse|/live/ai/generate-full-sse|/live/ai/generate-skeleton-sse"
          data-current-label={currentLabel}
          data-no-local-step-fallback="true"
          sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 1.5 }}
        >
          <AutorenewIcon sx={{ fontSize: 18, color: 'primary.main', animation: 'spin 1s linear infinite' }} />
          <Typography variant="body2" color="text.secondary" sx={{ fontSize: 12 }}>
            {getStepTitle(currentLabel)}
          </Typography>
        </Box>
      )}
      {filteredSteps.map((step, idx) => (
        <FlatStepItem
          key={step.stepKey ?? `step-${idx}`}
          ref={idx === filteredSteps.length - 1 ? lastStepRef : undefined}
          step={step}
          idx={idx}
          isLast={idx === filteredSteps.length - 1}
          justCompleted={justCompleted}
          expandedKeys={expandedKeys}
          onToggleExpand={onToggleExpand}
          onCopy={onCopy}
          onRetryStep={onRetryStep}
        />
      ))}
    </>
  )
})
