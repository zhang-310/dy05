import { Alert, Box, Chip, Paper, Stack, Typography } from '@mui/material'

export interface OfficialReferenceItem {
  kbName?: string
  refType?: string
  docId?: number
  chunkId?: number
  title?: string
  contentPreview?: string
  score?: number
}

interface OfficialReferencesPanelProps {
  references?: OfficialReferenceItem[] | null
  title?: string
  required?: boolean
  satisfied?: boolean
  status?: string
  maxItems?: number
  endpoint?: string
  testId?: string
}

function referenceTypeLabel(ref: OfficialReferenceItem) {
  if (ref.kbName === 'douyin_weigui' || ref.refType === 'violation_rule') return '违规规则'
  if (ref.refType === 'viral_pattern') return '爆款模式'
  if (ref.refType === 'performance_reflection') return '复盘经验'
  return '官方学习'
}

function statusLabel(required?: boolean, satisfied?: boolean, status?: string, count = 0) {
  if (required && !satisfied) return status || '缺少官方规则引用'
  if (required && satisfied) return '已满足官方规则引用'
  if (count > 0) return '已引用官方资料'
  return '未返回官方引用'
}

export function OfficialReferencesPanel({
  references,
  title = '引用的抖音官方规则',
  required,
  satisfied,
  status,
  maxItems = 6,
  endpoint,
  testId = 'official-references-panel',
}: OfficialReferencesPanelProps) {
  const items = Array.isArray(references) ? references : []
  const visibleItems = items.slice(0, maxItems)
  const missingRequired = required === true && satisfied !== true

  return (
    <Box
      data-testid={testId}
      data-source-endpoint={endpoint}
      data-official-ref-count={items.length}
      data-official-ref-required={required ? 'true' : 'false'}
      data-official-ref-satisfied={satisfied ? 'true' : 'false'}
      data-official-ref-status={status ?? ''}
    >
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
        <Typography variant="subtitle2">{title}</Typography>
        <Chip
          size="small"
          label={statusLabel(required, satisfied, status, items.length)}
          color={missingRequired ? 'error' : items.length > 0 ? 'success' : 'default'}
          variant={missingRequired ? 'filled' : 'outlined'}
        />
        {items.length > 0 && <Chip size="small" label={`${items.length} 条`} variant="outlined" />}
      </Stack>

      {missingRequired && items.length === 0 && (
        <Alert severity="error" sx={{ py: 0.5 }}>
          AI 未检索到 douyin_weigui 官方违规规则，当前结果不能判定为通过。
        </Alert>
      )}

      {visibleItems.length > 0 && (
        <Stack spacing={1}>
          {visibleItems.map((ref, index) => (
            <Paper
              key={`${ref.chunkId ?? ref.docId ?? index}-${index}`}
              variant="outlined"
              sx={{ p: 1, borderRadius: 1 }}
              data-kb-name={ref.kbName ?? ''}
              data-ref-type={ref.refType ?? ''}
            >
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 0.5 }}>
                <Chip
                  size="small"
                  label={referenceTypeLabel(ref)}
                  color={ref.kbName === 'douyin_weigui' || ref.refType === 'violation_rule' ? 'error' : 'info'}
                  variant="outlined"
                />
                {ref.kbName && <Chip size="small" label={ref.kbName} variant="outlined" />}
                <Typography variant="caption" color="text.secondary" sx={{ flex: 1, minWidth: 160 }}>
                  {ref.title || '抖音官方资料'}
                </Typography>
              </Stack>
              {ref.contentPreview && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  {ref.contentPreview}
                </Typography>
              )}
            </Paper>
          ))}
        </Stack>
      )}
    </Box>
  )
}

export default OfficialReferencesPanel
