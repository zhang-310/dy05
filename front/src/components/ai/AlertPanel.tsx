import { Box, Typography, Alert, Button } from '@mui/material'
import { Warning as WarningIcon, CheckCircle as CheckIcon } from '@mui/icons-material'

export interface AlertItem {
  id?: string
  component: string
  ok: boolean
  message?: string | null
  severity?: 'error' | 'warning' | 'info' | 'success'
}

export interface AlertPanelProps {
  /** 告警项列表 */
  items: AlertItem[]
  /** 标题 */
  title?: string
  /** 无告警时的成功文案 */
  successMessage?: string
  /** 无数据时的文案 */
  emptyMessage?: string
  /** 是否显示一键处理按钮（需配合 onResolve） */
  showResolve?: boolean
  /** 一键处理回调 */
  onResolve?: (item: AlertItem) => void
}

const COMPONENT_LABELS: Record<string, string> = {
  milvus: 'Milvus',
  elasticsearch: 'Elasticsearch',
  redis: 'Redis',
  postgresql: 'PostgreSQL',
}

function getComponentLabel(component: string): string {
  return COMPONENT_LABELS[component] ?? component
}

/**
 * 告警面板 - 按严重程度展示异常/正常状态
 * @param items 告警项（component, ok, message）
 * @param title 标题，默认「异常告警」
 * @param successMessage 全部正常时文案
 * @param emptyMessage 无数据时文案
 * @param showResolve 是否显示一键处理按钮
 * @param onResolve 一键处理回调
 */
export function AlertPanel({
  items,
  title = '异常告警',
  successMessage = '运行正常',
  emptyMessage = '暂无告警',
  showResolve = false,
  onResolve,
}: AlertPanelProps) {
  const unhealthyItems = items.filter((h) => !h.ok)

  return (
    <Box>
      <Typography
        variant="subtitle1"
        fontWeight={600}
        sx={{
          mb: 2,
          display: 'flex',
          alignItems: 'center',
          gap: 0.5,
          color: unhealthyItems.length > 0 ? 'error.main' : 'success.main',
        }}
      >
        {unhealthyItems.length > 0 ? (
          <WarningIcon fontSize="small" />
        ) : (
          <CheckIcon fontSize="small" />
        )}
        {title}
      </Typography>
      {unhealthyItems.length > 0 ? (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          {unhealthyItems.map((h) => (
            <Alert
              key={h.component}
              severity={h.severity ?? 'warning'}
              sx={{ py: 0.5 }}
              action={
                showResolve && onResolve ? (
                  <Button color="inherit" size="small" onClick={() => onResolve(h)}>
                    处理
                  </Button>
                ) : undefined
              }
            >
              <Typography variant="body2">
                {getComponentLabel(h.component)} 异常
                {h.message && ` · ${h.message}`}
              </Typography>
            </Alert>
          ))}
        </Box>
      ) : items.length > 0 ? (
        <Alert severity="success" sx={{ py: 0.5 }}>
          <Typography variant="body2">{successMessage}</Typography>
        </Alert>
      ) : (
        <Typography color="text.secondary" variant="body2">
          {emptyMessage}
        </Typography>
      )}
    </Box>
  )
}
